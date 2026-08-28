package com.haru.haruverse.external.tmdb;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TMDB 수집 API — <b>관리자 전용</b>.
 *
 * <p>SecurityConfig 에서 {@code /api/collect/**} 를 ADMIN 으로 제한한다.
 * 외부 API 를 대신 호출하는 엔드포인트라 아무나 부르면 쿼터가 소진된다.
 */
@RestController
@RequestMapping("/api/collect/tmdb")
public class TmdbCollectController {

    private final TmdbTitleCollectService collectService;

    public TmdbCollectController(TmdbTitleCollectService collectService) {
        this.collectService = collectService;
    }

    /**
     * 한국어 제목 수집 — POST /api/collect/tmdb/titles?limit=100
     *
     * <p>⚠️ 작품 1건마다 API 를 1~2회 부르고 사이에 간격을 둔다.
     * limit=86 이면 <b>30초 안팎</b> 걸린다. 브라우저에서 부르면 타임아웃될 수 있다.
     *
     * @param skipCollected 기본 true — 이미 한국어 제목이 있으면 건너뛴다.
     *                      매칭 규칙을 고친 뒤 전부 다시 받고 싶으면 false.
     */
    @PostMapping("/titles")
    public ResponseEntity<TmdbTitleCollectService.CollectResult> collectTitles(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "true") boolean skipCollected) {
        return ResponseEntity.ok(collectService.collect(limit, skipCollected));
    }
}
