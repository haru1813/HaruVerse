package com.haru.haruverse.external.rawg;

import com.haru.haruverse.external.rawg.dto.RawgGame;
import com.haru.haruverse.external.rawg.dto.RawgPageResponse;
import com.haru.haruverse.global.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAWG에서 게임 목록을 받아 work 테이블로 옮기는 수집 오케스트레이터.
 *
 * <p>JikanCollectService와 동일한 원칙:
 * <ul>
 *   <li>외부 호출은 트랜잭션 <b>밖</b>, 저장은 건별 짧은 트랜잭션(RawgWorkWriter)</li>
 *   <li>페이지 단위 부분 실패를 견디고, 연속 실패가 쌓이면 중단</li>
 * </ul>
 */
@Service
public class RawgCollectService {

    private static final Logger log = LoggerFactory.getLogger(RawgCollectService.class);

    private static final int MAX_CONSECUTIVE_PAGE_FAILURES = 3;
    /** RAWG는 Jikan만큼 빡빡하지 않지만(월 20,000) 예의상 간격을 둔다 */
    private static final long DELAY_MS = 250;

    private final RawgClient rawgClient;
    private final RawgWorkWriter workWriter;

    public RawgCollectService(RawgClient rawgClient, RawgWorkWriter workWriter) {
        this.rawgClient = rawgClient;
        this.workWriter = workWriter;
    }

    public record CollectResult(int fetched, int created, int updated, int failed, int failedPages) {}

    /**
     * 게임 목록 수집.
     *
     * @param ordering RAWG 정렬 파라미터 (예: "-metacritic", "-rating", "-released")
     */
    public CollectResult collectGames(int pages, int pageSize, String ordering) {
        int fetched = 0, created = 0, updated = 0, failed = 0, failedPages = 0;
        int consecutiveFailures = 0;

        for (int page = 1; page <= pages; page++) {
            RawgPageResponse res;
            try {
                res = rawgClient.fetchGames(page, pageSize, ordering);   // 트랜잭션 밖
            } catch (ExternalApiException e) {
                failedPages++;
                consecutiveFailures++;
                log.warn("페이지 {} 호출 실패 ({}회 연속): {}", page, consecutiveFailures, e.getMessage());
                if (consecutiveFailures >= MAX_CONSECUTIVE_PAGE_FAILURES) {
                    log.warn("연속 {}회 실패 — RAWG 장애로 판단하고 중단합니다.", consecutiveFailures);
                    break;
                }
                sleep();
                continue;
            }

            consecutiveFailures = 0;
            if (res == null) break;

            for (RawgGame game : res.safeResults()) {
                fetched++;
                try {
                    if (workWriter.upsert(game)) created++;
                    else updated++;
                } catch (Exception e) {
                    failed++;
                    log.warn("게임 저장 실패 (id={}): {}", game.id(), e.getMessage());
                }
            }

            if (!res.hasNext()) break;
            sleep();
        }

        log.info("RAWG 수집 완료 — 조회 {}건, 신규 {}건, 갱신 {}건, 저장실패 {}건, 실패페이지 {}개",
                fetched, created, updated, failed, failedPages);
        return new CollectResult(fetched, created, updated, failed, failedPages);
    }

    /**
     * 지정한 게임 ID들의 <b>상세</b>를 수집.
     *
     * <p>목록 API에는 줄거리·개발사가 없으므로, 이미 수집한 게임을
     * 상세로 한 번 더 훑어 채우는 용도로도 쓴다.
     */
    public CollectResult collectByIds(List<Long> gameIds) {
        int fetched = 0, created = 0, updated = 0, failed = 0;

        for (Long id : gameIds) {
            try {
                RawgGame game = rawgClient.fetchGame(id);   // 트랜잭션 밖
                if (game == null) {
                    failed++;
                    continue;
                }
                fetched++;
                if (workWriter.upsert(game)) created++;
                else updated++;
            } catch (Exception e) {
                failed++;
                log.warn("게임 단건 수집 실패 (id={}): {}", id, e.getMessage());
            }
            sleep();
        }

        log.info("RAWG 단건 수집 완료 — 조회 {}건, 신규 {}건, 갱신 {}건, 실패 {}건", fetched, created, updated, failed);
        return new CollectResult(fetched, created, updated, failed, 0);
    }

    private void sleep() {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
