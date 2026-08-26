package com.haru.haruverse.external.starrail;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스타레일 캐릭터 수집 — 다른 수집 API와 같이 인증이 필요하다.
 */
@RestController
@RequestMapping("/api/collect/starrail")
public class StarRailCollectController {

    private final StarRailCollectService collectService;

    public StarRailCollectController(StarRailCollectService collectService) {
        this.collectService = collectService;
    }

    /**
     * POST /api/collect/starrail/characters?workId=257
     *
     * @param workId 캐릭터를 묶을 작품 id (붕괴: 스타레일)
     */
    @PostMapping("/characters")
    public ResponseEntity<StarRailCollectService.CollectResult> collectCharacters(
            @RequestParam Long workId) {
        return ResponseEntity.ok(collectService.collect(workId));
    }
}
