package com.haru.haruverse.external.tmdb;

import com.haru.haruverse.external.tmdb.dto.TmdbSearchResponse;
import com.haru.haruverse.external.tmdb.dto.TmdbSearchResponse.Result;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 한국어 제목 수집 — TMDB.
 *
 * <p><b>왜 필요한가</b>
 * Jikan·RAWG 가 주는 제목이 전부 영문이라, 한국인이 "프리렌"으로 검색하면 0건이었다.
 * Elasticsearch 를 붙여도 해결되지 않는 <b>데이터 문제</b>다.
 *
 * <p><b>대상은 애니메이션뿐</b> — 게임은 TMDB 에 없다.
 *
 * <p><b>★확신이 없으면 채우지 않는다★</b>
 * 매칭에 실패한 작품은 그냥 넘어간다. 틀린 한글 제목이 붙으면 사용자는
 * "매칭이 실패했구나"가 아니라 "이 사이트 데이터가 엉망이구나"라고 판단한다.
 */
@Service
public class TmdbTitleCollectService {

    private static final Logger log = LoggerFactory.getLogger(TmdbTitleCollectService.class);

    /** TMDB 요청 간격 — 공식 제한은 넉넉하지만 예의상 간격을 둔다 */
    private static final long DELAY_MS = 120;
    /** 연속 실패 시 중단 — MAL 장애 때 얻은 교훈(끝까지 돌면서 계속 실패하지 않게) */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final TmdbClient client;
    private final TmdbTitleMatcher matcher;
    private final WorkRepository workRepository;
    /** ★별도 빈★ 같은 클래스에서 부르면 @Transactional 이 적용되지 않는다 */
    private final TmdbTitleWriter writer;

    public TmdbTitleCollectService(TmdbClient client,
                                   TmdbTitleMatcher matcher,
                                   WorkRepository workRepository,
                                   TmdbTitleWriter writer) {
        this.client = client;
        this.matcher = matcher;
        this.workRepository = workRepository;
        this.writer = writer;
    }

    /** 수집 결과 */
    public record CollectResult(
            int works,      // 시도한 작품 수
            int matched,    // 한국어 제목을 채운 수
            int unmatched,  // 후보를 못 찾아 건너뛴 수
            int failed,     // 호출 실패
            int skipped,    // 이미 채워져 있어 건너뜀
            boolean stopped // 연속 실패로 중단됐는지
    ) {}

    /**
     * 애니메이션의 한국어 제목을 채운다.
     *
     * <p>★트랜잭션을 메서드 전체에 걸지 않는다★
     * 작품 하나당 외부 API 를 부르는데, 그걸 한 트랜잭션으로 묶으면
     * 커넥션을 수십 초 동안 붙잡고 있게 된다. 작품 단위로 짧게 커밋한다.
     *
     * @param limit         최대 처리 개수
     * @param skipCollected true면 이미 한국어 제목이 있는 작품은 건너뛴다
     */
    public CollectResult collect(int limit, boolean skipCollected) {
        List<Work> targets = workRepository.findAll().stream()
                .filter(w -> w.getType() == WorkType.ANIME) // 게임은 TMDB 에 없다
                .filter(w -> !skipCollected || w.getTitleKo() == null)
                .limit(Math.max(limit, 1))
                .toList();

        int matched = 0, unmatched = 0, failed = 0, consecutiveFailures = 0;

        for (Work work : targets) {
            try {
                String koTitle = findKoreanTitle(work);
                if (koTitle == null) {
                    unmatched++;
                    log.debug("한국어 제목 매칭 실패: {}", work.getTitle());
                } else {
                    writer.save(work.getId(), koTitle);
                    matched++;
                }
                consecutiveFailures = 0;

            } catch (Exception e) {
                failed++;
                consecutiveFailures++;
                log.warn("한국어 제목 수집 실패: [{}] {}", work.getTitle(), e.toString());

                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    log.error("연속 {}회 실패 — 수집을 중단합니다.", MAX_CONSECUTIVE_FAILURES);
                    return new CollectResult(targets.size(), matched, unmatched, failed, 0, true);
                }
            }
            sleep();
        }
        return new CollectResult(targets.size(), matched, unmatched, failed, 0, false);
    }

    /**
     * 두 단계로 찾는다.
     *
     * <p><b>★검색은 en-US, 한국어 제목은 상세 조회★</b>
     * 처음엔 {@code ko-KR} 로 한 번만 검색했는데, 그러면 {@code original_name} 이
     * <b>일본어</b>로 온다(葬送のフリーレン). 우리가 가진 건 영문 제목이라 비교가 안 돼서
     * <b>원제가 라틴 문자인 작품만 매칭됐다</b>(프리렌이 통째로 누락됐다).
     * 그래서 매칭은 영문으로 하고, 한국어 제목은 그 id 로 다시 받는다.
     *
     * <p>애니는 대부분 TV 시리즈다. 극장판만 영화 쪽이라 TV 에서 못 찾았을 때만 부른다.
     */
    private String findKoreanTitle(Work work) {
        Integer year = work.getReleaseDate() == null ? null : work.getReleaseDate().getYear();
        String title = work.getTitle();

        // ── 1단계: 영문 제목 그대로 검색 ──
        Result hit = pickFrom(client.searchTv(title, "en-US"), title, year);
        boolean isTv = hit != null;
        if (hit == null) {
            hit = pickFrom(client.searchMovie(title, "en-US"), title, year);
        }

        // ── 1-b단계: 시즌 표기를 떼고 재시도 ──
        //   ★TMDB 는 시즌을 별도 작품으로 두지 않는다★ 시리즈 하나 안의 시즌으로 관리한다.
        //   그래서 "Attack on Titan Season 3 Part 2" 로는 아무것도 안 걸린다.
        //   시즌 표기를 떼고 시리즈를 찾으면 "진격의 거인" 을 얻는다.
        //   ⚠️ 이때는 연도 관문을 끈다 — 시리즈 시작 연도(2013)와 시즌 방영 연도(2019)가 다르다.
        //      대신 제목만으로 판단하므로, 원래도 제목이 안 맞으면 여전히 걸러진다.
        if (hit == null) {
            String base = stripSeason(title);
            if (!base.equalsIgnoreCase(title)) {
                hit = pickFrom(client.searchTv(base, "en-US"), base, null);
                isTv = hit != null;
                if (hit == null) {
                    hit = pickFrom(client.searchMovie(base, "en-US"), base, null);
                }
            }
        }

        if (hit == null || hit.id() == null) return null;

        // ── 2단계: 그 id 로 한국어 제목을 받는다 ──
        Result korean = isTv
                ? client.fetchTvDetail(hit.id(), "ko-KR")
                : client.fetchMovieDetail(hit.id(), "ko-KR");
        if (korean == null) return null;

        String ko = korean.localizedTitle();
        // ★TMDB 는 한국어 제목이 없으면 영문을 그대로 준다★
        //   그걸 저장하면 title 과 똑같은 값이 titleKo 에 들어가 아무 의미가 없다.
        if (ko == null || ko.isBlank() || ko.equalsIgnoreCase(title)) return null;
        // 한글이 한 글자도 없으면 현지화가 안 된 것이다
        if (!ko.matches(".*[가-힣].*")) return null;

        return ko;
    }

    /**
     * 시즌·파트 표기를 떼어낸다.
     *
     * <p>"Attack on Titan Season 3 Part 2" → "Attack on Titan"
     * <p>"Gintama Season 4" → "Gintama"
     *
     * <p>떼어낸 뒤 시리즈의 한국어 제목을 쓰므로, 같은 시리즈의 시즌들은
     * <b>같은 titleKo 를 갖게 된다</b>. 검색 목적에는 그게 오히려 낫다 —
     * "진격의 거인"으로 검색하면 시즌이 전부 나온다.
     */
    static String stripSeason(String title) {
        return title
                .replaceAll("(?i)\\s+(season|part|cour)\\s*\\d+.*$", "")
                .replaceAll("(?i)\\s+(final\\s+season|the\\s+final).*$", "")
                .replaceAll("(?i)\\s+\\d+(st|nd|rd|th)\\s+season.*$", "")
                .trim();
    }

    private Result pickFrom(TmdbSearchResponse response, String title, Integer year) {
        if (response == null) return null;
        return matcher.pick(new ArrayList<>(response.safeResults()), title, year);
    }

    private void sleep() {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
