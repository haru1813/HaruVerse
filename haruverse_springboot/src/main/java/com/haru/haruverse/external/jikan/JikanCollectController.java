package com.haru.haruverse.external.jikan;

import com.haru.haruverse.external.jikan.JikanCollectService.CollectResult;
import com.haru.haruverse.global.exception.TooManyItemsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 데이터 수집 트리거 API.
 *
 * <p>SecurityConfig의 기본 규칙(anyRequest().authenticated())에 걸리므로
 * <b>JWT 토큰이 있어야</b> 호출할 수 있다. 아무나 부르면 외부 API 요청 제한을
 * 소진시키거나 DB를 채워버릴 수 있기 때문.
 *
 * <p>TODO(하루): 지금은 로그인한 회원이면 누구나 호출 가능하다.
 * Member에 role(USER/ADMIN)을 넣고 @PreAuthorize("hasRole('ADMIN')")로 좁히는 게 정석.
 */
@RestController
@RequestMapping("/api/collect/jikan")
public class JikanCollectController {

    /** 한 번에 받아올 수 있는 작품 수 (Jikan 분당 60회 제한을 고려한 값) */
    private static final int MAX_IDS_PER_REQUEST = 30;

    private final JikanCollectService collectService;
    private final JikanCharacterCollectService characterCollectService;

    public JikanCollectController(JikanCollectService collectService,
                                  JikanCharacterCollectService characterCollectService) {
        this.collectService = collectService;
        this.characterCollectService = characterCollectService;
    }

    /**
     * 인기 애니 수집 — POST /api/collect/jikan/top?pages=2&limit=25
     */
    @PostMapping("/top")
    public ResponseEntity<CollectResult> collectTop(
            @RequestParam(defaultValue = "1") int pages,
            @RequestParam(defaultValue = "25") int limit) {
        return ResponseEntity.ok(collectService.collectTopAnime(clampPages(pages), clampLimit(limit)));
    }

    /**
     * 분기별 애니 수집 — POST /api/collect/jikan/season?year=2026&season=spring&pages=2
     */
    @PostMapping("/season")
    public ResponseEntity<CollectResult> collectSeason(
            @RequestParam int year,
            @RequestParam String season,
            @RequestParam(defaultValue = "1") int pages,
            @RequestParam(defaultValue = "25") int limit) {
        return ResponseEntity.ok(
                collectService.collectSeasonAnime(year, season, clampPages(pages), clampLimit(limit)));
    }

    /**
     * 지정한 MAL ID들만 수집 — POST /api/collect/jikan/ids?ids=52991,61316
     *
     * <p>목록 API가 불안정할 때의 우회 경로 겸, 특정 작품만 갱신하는 용도.
     */
    @PostMapping("/ids")
    public ResponseEntity<CollectResult> collectByIds(@RequestParam List<Long> ids) {
        // 한 번에 너무 많이 요청하면 Jikan 분당 제한(60회)에 걸린다
        // ★조용히 자르지 않는다★ (RawgCollectController와 같은 이유)
        if (ids.size() > MAX_IDS_PER_REQUEST) {
            throw new TooManyItemsException(ids.size(), MAX_IDS_PER_REQUEST);
        }
        return ResponseEntity.ok(collectService.collectByIds(ids));
    }

    // 요청 제한(분당 60회)을 한 번에 태우지 않도록 상한을 둔다
    private int clampPages(int pages) {
        return Math.max(1, Math.min(pages, 10));
    }

    // Jikan의 페이지당 최대치가 25
    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 25));
    }

    /**
     * 캐릭터 수집 — POST /api/collect/jikan/characters?limit=100
     *
     * <p>저장된 애니 작품을 앞에서부터 훑으며 등장인물을 가져온다.
     * ⚠️ 작품 1건마다 API를 1회씩 부르고 분당 60회 제한을 지키므로,
     * limit=86이면 <b>1분 40초쯤 걸린다.</b> 브라우저에서 부르면 타임아웃될 수 있다.
     */
    @PostMapping("/characters")
    public ResponseEntity<JikanCharacterCollectService.CollectResult> collectCharacters(
            @RequestParam(defaultValue = "20") int limit,
            // 기본은 "아직 안 받은 것만" — MAL 장애로 중단됐을 때 이어서 받는 게 흔한 사용법이다
            @RequestParam(defaultValue = "true") boolean skipCollected) {
        return ResponseEntity.ok(characterCollectService.collect(limit, skipCollected));
    }
}
