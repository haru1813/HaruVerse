package com.haru.haruverse.external.jikan;

import com.haru.haruverse.external.jikan.dto.JikanAnime;
import com.haru.haruverse.external.jikan.dto.JikanPageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Jikan에서 애니 목록을 받아 work 테이블로 옮기는 수집 오케스트레이터.
 *
 * <p><b>트랜잭션 경계 설계가 핵심이다.</b>
 * 외부 API 호출(수 초가 걸릴 수 있음)을 트랜잭션 안에서 하면 그동안 DB 커넥션을
 * 붙잡고 있게 되어 커넥션 풀이 말라버린다.
 * → 이 클래스에는 @Transactional을 붙이지 않고(호출은 트랜잭션 밖),
 *   저장은 JikanWorkWriter가 <b>건별 짧은 트랜잭션</b>으로 처리한다.
 */
@Service
public class JikanCollectService {

    private static final Logger log = LoggerFactory.getLogger(JikanCollectService.class);

    /** Jikan 요청 제한(초당 3회)을 넘지 않도록 페이지 사이에 두는 간격 */
    private static final long RATE_LIMIT_DELAY_MS = 400;

    private final JikanClient jikanClient;
    private final JikanWorkWriter workWriter;

    public JikanCollectService(JikanClient jikanClient, JikanWorkWriter workWriter) {
        this.jikanClient = jikanClient;
        this.workWriter = workWriter;
    }

    /** 페이지 호출이 연속으로 이만큼 실패하면 중단 (외부 API가 완전히 죽은 경우) */
    private static final int MAX_CONSECUTIVE_PAGE_FAILURES = 3;

    /**
     * 수집 결과 요약.
     *
     * @param fetched     외부에서 받아온 작품 수
     * @param created     새로 저장된 수
     * @param updated     기존 것을 갱신한 수
     * @param failed      저장에 실패한 작품 수
     * @param failedPages 호출 자체가 실패한 페이지 수 (외부 API 장애)
     */
    public record CollectResult(int fetched, int created, int updated, int failed, int failedPages) {}

    /**
     * 인기 애니 수집.
     *
     * @param pages 가져올 페이지 수
     * @param limit 페이지당 건수 (Jikan 최대 25)
     */
    public CollectResult collectTopAnime(int pages, int limit) {
        return collect(pages, page -> jikanClient.fetchTopAnime(page, limit));
    }

    /** 분기별 애니 수집 (예: 2026, "spring") */
    public CollectResult collectSeasonAnime(int year, String season, int pages, int limit) {
        return collect(pages, page -> jikanClient.fetchSeasonAnime(year, season, page, limit));
    }

    /**
     * 지정한 MAL ID들만 수집 — 목록 API가 불안정할 때의 우회 경로이자
     * 특정 작품만 최신값으로 갱신할 때 쓰는 경로.
     */
    public CollectResult collectByIds(List<Long> malIds) {
        int fetched = 0, created = 0, updated = 0, failed = 0;

        for (Long malId : malIds) {
            try {
                JikanAnime anime = jikanClient.fetchAnime(malId); // 트랜잭션 밖
                if (anime == null) {
                    failed++;
                    continue;
                }
                fetched++;
                if (workWriter.upsert(anime)) created++;
                else updated++;
            } catch (Exception e) {
                failed++;
                log.warn("단건 수집 실패 (mal_id={}): {}", malId, e.getMessage());
            }
            sleepForRateLimit();
        }

        log.info("Jikan 단건 수집 완료 — 조회 {}건, 신규 {}건, 갱신 {}건, 실패 {}건", fetched, created, updated, failed);
        return new CollectResult(fetched, created, updated, failed, 0);
    }

    /**
     * 페이지를 순회하며 수집하는 공통 흐름.
     *
     * <p><b>부분 실패를 견딘다.</b> 외부 API는 간헐적으로 5xx를 뱉는다(실제로 Jikan은
     * MyAnimeList가 흔들리면 504를 준다). 한 페이지가 실패했다고 전체를 중단하면
     * 이미 저장한 앞 페이지의 성과도 결과로 못 돌려준다.
     * → 페이지 단위로 예외를 잡고 다음 페이지로 넘어가되,
     *   연속 실패가 누적되면 외부 API가 죽은 것으로 보고 중단한다.
     */
    private CollectResult collect(int pages, PageFetcher fetcher) {
        int fetched = 0, created = 0, updated = 0, failed = 0, failedPages = 0;
        int consecutiveFailures = 0;

        for (int page = 1; page <= pages; page++) {
            JikanPageResponse res;
            try {
                // ① 외부 호출 — 트랜잭션 밖
                res = fetcher.fetch(page);
            } catch (JikanApiException e) {
                failedPages++;
                consecutiveFailures++;
                log.warn("페이지 {} 호출 실패 ({}회 연속): {}", page, consecutiveFailures, e.getMessage());

                if (consecutiveFailures >= MAX_CONSECUTIVE_PAGE_FAILURES) {
                    log.warn("연속 {}회 실패 — 외부 API 장애로 판단하고 수집을 중단합니다.", consecutiveFailures);
                    break;
                }
                sleepForRateLimit();
                continue; // 다음 페이지 시도
            }

            consecutiveFailures = 0; // 성공했으니 연속 실패 카운터 초기화
            if (res == null) break;

            for (JikanAnime anime : res.safeData()) {
                fetched++;
                try {
                    // ② 저장 — 다른 빈이므로 프록시를 거쳐 @Transactional이 적용된다.
                    //    한 건이 실패해도 그 건만 롤백되고 나머지 수집은 계속된다.
                    if (workWriter.upsert(anime)) created++;
                    else updated++;
                } catch (Exception e) {
                    failed++;
                    log.warn("작품 저장 실패 (mal_id={}): {}", anime.malId(), e.getMessage());
                }
            }

            if (!res.hasNext()) break;
            sleepForRateLimit();
        }

        log.info("Jikan 수집 완료 — 조회 {}건, 신규 {}건, 갱신 {}건, 저장실패 {}건, 실패페이지 {}개",
                fetched, created, updated, failed, failedPages);
        return new CollectResult(fetched, created, updated, failed, failedPages);
    }

    private void sleepForRateLimit() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태를 삼키지 않고 복원
        }
    }

    /** 페이지 번호를 받아 응답을 돌려주는 함수형 인터페이스 (top/season 공통화용) */
    @FunctionalInterface
    private interface PageFetcher {
        JikanPageResponse fetch(int page);
    }
}
