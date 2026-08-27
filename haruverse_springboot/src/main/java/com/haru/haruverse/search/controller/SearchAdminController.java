package com.haru.haruverse.search.controller;

import com.haru.haruverse.search.service.WorkIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 색인 관리 API — <b>관리자 전용</b>.
 *
 * <p>수집 API와 같은 이유로 잠근다. 재색인은 DB를 통째로 읽어 ES에 밀어 넣는 작업이라
 * 아무나 반복 호출하면 그 자체가 부하가 된다.
 * (SecurityConfig 에서 {@code POST /api/search/**} 를 ADMIN 으로 제한한다.
 *  검색 자체는 {@code GET} 이라 공개다)
 */
@RestController
@RequestMapping("/api/search")
public class SearchAdminController {

    private final WorkIndexService indexService;

    public SearchAdminController(WorkIndexService indexService) {
        this.indexService = indexService;
    }

    /**
     * 전량 재색인 — POST /api/search/reindex
     *
     * @param recreate true 면 색인을 지우고 다시 만든 뒤 채운다.
     *                 ES 매핑은 한 번 만들면 <b>바꿀 수 없는 필드가 있어서</b>,
     *                 필드 타입이나 분석기를 고쳤을 때는 이 옵션이 필요하다.
     */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex(
            @RequestParam(defaultValue = "false") boolean recreate) {

        if (recreate && !indexService.recreateIndex()) {
            return ResponseEntity.status(503).body(Map.of(
                    "ok", false,
                    "message", "색인을 다시 만들지 못했습니다. Elasticsearch 상태를 확인하세요."));
        }

        int indexed = indexService.reindexAll();
        if (indexed < 0) {
            // ★500이 아니라 503★ 서버 코드가 잘못된 게 아니라 의존하는 외부 시스템이 없는 상태다
            return ResponseEntity.status(503).body(Map.of(
                    "ok", false,
                    "message", "Elasticsearch에 연결하지 못했습니다. 검색은 DB 조회로 동작합니다."));
        }
        return ResponseEntity.ok(Map.of("ok", true, "indexed", indexed, "recreated", recreate));
    }
}
