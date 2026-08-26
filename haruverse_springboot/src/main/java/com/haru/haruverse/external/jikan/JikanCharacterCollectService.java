package com.haru.haruverse.external.jikan;

import com.haru.haruverse.external.jikan.dto.JikanCharacterEntry;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.character.repository.WorkCharacterRepository;
import com.haru.haruverse.work.repository.WorkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 캐릭터 수집 — 저장된 애니 작품을 훑으며 등장인물을 가져온다.
 *
 * <p><b>이 클래스는 트랜잭션을 열지 않는다.</b>
 * 외부 API 호출은 느리고 실패도 잦은데, 트랜잭션 안에서 부르면
 * DB 커넥션을 붙잡은 채 응답을 기다리게 되어 커넥션 풀이 마른다.
 * → 호출은 여기서, 저장은 {@link JikanCharacterWriter}(별도 클래스)에서.
 */
@Service
public class JikanCharacterCollectService {

    private static final Logger log = LoggerFactory.getLogger(JikanCharacterCollectService.class);

    /**
     * 요청 간격.
     *
     * <p>★기존 수집(400ms)을 그대로 쓰면 안 된다★
     * Jikan 제한은 <b>초당 3회 <u>그리고</u> 분당 60회</b>다.
     * 400ms면 초당 2.5회로 첫 조건은 통과하지만 분당 150회가 되어 두 번째에 걸린다.
     * 기존 수집은 페이지 단위라 한 번에 5~10회뿐이어서 드러나지 않았을 뿐이다.
     * 캐릭터는 작품마다 1회씩 수십 번을 연속으로 보내므로 분당 60회에 맞춘다.
     */
    private static final long RATE_LIMIT_DELAY_MS = 1_100;

    /** 연속으로 이만큼 실패하면 중단 — MAL이 죽은 상황에서 끝까지 두드리지 않게 */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final JikanClient jikanClient;
    private final JikanCharacterWriter writer;
    private final WorkRepository workRepository;
    private final WorkCharacterRepository workCharacterRepository;

    public JikanCharacterCollectService(JikanClient jikanClient,
                                        JikanCharacterWriter writer,
                                        WorkRepository workRepository,
                                        WorkCharacterRepository workCharacterRepository) {
        this.jikanClient = jikanClient;
        this.writer = writer;
        this.workRepository = workRepository;
        this.workCharacterRepository = workCharacterRepository;
    }

    /**
     * @param works      훑은 작품 수
     * @param created    새로 만든 캐릭터
     * @param updated    이미 있어 갱신한 캐릭터
     * @param linked     새로 만든 작품-캐릭터 연결
     * @param failed     API 호출이 실패한 작품
     * @param skipped    malId가 없어 건너뛴 작품
     * @param stopped    연속 실패로 중간에 멈췄는지
     */
    public record CollectResult(int works, int created, int updated, int linked,
                                int failed, int skipped, boolean stopped) {}

    /**
     * 애니 작품 앞에서부터 limit개의 캐릭터를 수집한다.
     *
     * <p>캐릭터 정보는 Jikan에만 있으므로 source=JIKAN인 작품만 대상이다.
     * (RAWG 게임에는 캐릭터 API가 없다)
     *
     * @param skipCollected 이미 캐릭터가 있는 작품을 건너뛸지.
     *        <p>★재시도할 때 중요하다★ MyAnimeList가 자주 죽어(504) 수집이 중간에 멈추는데,
     *        그대로 다시 돌리면 앞서 성공한 작품부터 다시 호출한다.
     *        분당 60회 제한 안에서 그 시간은 그대로 낭비다.
     *        전체를 갱신하고 싶을 때만 false로 둔다.
     */
    public CollectResult collect(int limit, boolean skipCollected) {
        List<Work> works = workRepository
                .findBySource(WorkSource.JIKAN, PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "id")))
                .getContent();

        int created = 0, updated = 0, linked = 0, failed = 0, skipped = 0, consecutive = 0;
        int processed = 0;
        boolean stopped = false;

        for (Work work : works) {
            // 이미 수집된 작품은 건너뛴다 (재시도 시 앞부분을 다시 호출하지 않도록)
            if (skipCollected && workCharacterRepository.countByWorkId(work.getId()) > 0) {
                skipped++;
                continue;
            }

            // ★externalId는 "jikan-52991" 형식이다★ 숫자로 바로 파싱하면 전부 건너뛴다.
            //   접두사 해석은 WorkSource가 담당한다.
            Long malId = WorkSource.JIKAN.extractExternalKey(work.getExternalId());
            if (malId == null) {
                log.warn("externalId 형식이 맞지 않아 건너뜀: workId={} externalId={}",
                        work.getId(), work.getExternalId());
                skipped++;
                continue;
            }

            try {
                List<JikanCharacterEntry> entries = jikanClient.fetchCharacters(malId);
                JikanCharacterWriter.WriteResult r = writer.upsert(work.getId(), entries);

                created += r.created();
                updated += r.updated();
                linked += r.linked();
                processed++;
                consecutive = 0;
                log.info("캐릭터 수집: [{}] {}건 (신규 {} · 연결 {})",
                        work.getTitle(), entries.size(), r.created(), r.linked());

            } catch (Exception e) {
                failed++;
                consecutive++;
                log.warn("캐릭터 수집 실패: [{}] {}", work.getTitle(), e.getMessage());
                if (consecutive >= MAX_CONSECUTIVE_FAILURES) {
                    // 여기서 예외를 던지면 이미 저장한 것까지 없던 일이 된다.
                    // → 멈추되 그때까지의 결과는 돌려준다.
                    log.error("연속 {}회 실패 — 수집을 중단합니다.", consecutive);
                    stopped = true;
                    break;
                }
            }

            sleepForRateLimit();
        }

        return new CollectResult(processed, created, updated, linked, failed, skipped, stopped);
    }

    private void sleepForRateLimit() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태를 삼키지 않고 복원
        }
    }
}
