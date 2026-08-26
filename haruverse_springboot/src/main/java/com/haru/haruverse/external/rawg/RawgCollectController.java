package com.haru.haruverse.external.rawg;

import com.haru.haruverse.external.rawg.RawgCollectService.CollectResult;
import com.haru.haruverse.global.exception.MissingApiKeyException;
import com.haru.haruverse.global.exception.TooManyItemsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAWG 게임 수집 트리거 API.
 *
 * <p>SecurityConfig 기본 규칙(anyRequest().authenticated())에 걸리므로 JWT 필요.
 */
@RestController
@RequestMapping("/api/collect/rawg")
public class RawgCollectController {

    /** 한 번에 상세를 받아올 수 있는 게임 수 (RAWG 요청 제한과 응답 시간을 고려한 값) */
    private static final int MAX_IDS_PER_REQUEST = 40;

    private final RawgCollectService collectService;
    private final RawgClient rawgClient;

    public RawgCollectController(RawgCollectService collectService, RawgClient rawgClient) {
        this.collectService = collectService;
        this.rawgClient = rawgClient;
    }

    /**
     * 게임 목록 수집 — POST /api/collect/rawg/games?pages=5&pageSize=20&ordering=-metacritic
     *
     * @param ordering -metacritic(메타크리틱 높은순) · -rating · -released(최신순)
     */
    @PostMapping("/games")
    public ResponseEntity<CollectResult> collectGames(
            @RequestParam(defaultValue = "1") int pages,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "-metacritic") String ordering) {
        requireKey();
        return ResponseEntity.ok(
                collectService.collectGames(clamp(pages, 1, 20), clamp(pageSize, 1, 40), ordering));
    }

    /** 지정 게임의 상세 수집 (줄거리·개발사 채우기) — POST /api/collect/rawg/ids?ids=3498,4200 */
    @PostMapping("/ids")
    public ResponseEntity<CollectResult> collectByIds(@RequestParam List<Long> ids) {
        requireKey();
        // ★조용히 자르지 않는다★ 넘치면 거절한다 — 자르면 호출한 쪽이
        // 나머지가 버려진 걸 모른 채 "다 됐다"고 여긴다
        if (ids.size() > MAX_IDS_PER_REQUEST) {
            throw new TooManyItemsException(ids.size(), MAX_IDS_PER_REQUEST);
        }
        return ResponseEntity.ok(collectService.collectByIds(ids));
    }

    /**
     * 키가 없으면 502(외부 API 불가)로 나가버려 원인 파악이 어렵다.
     * → 호출 전에 미리 확인해 명확한 메시지를 준다.
     */
    private void requireKey() {
        if (!rawgClient.hasApiKey()) {
            throw new MissingApiKeyException(
                    "RAWG API 키가 설정되지 않았습니다. 환경변수 RAWG_API_KEY를 지정한 뒤 서버를 재시작해주세요.");
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, max));
    }
}
