package com.haru.haruverse.work.controller;

import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.work.dto.WorkDetailResponse;
import com.haru.haruverse.work.dto.WorkResponse;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.service.WorkService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 작품 API — 설계문서 ④ 3장(work).
 *
 * <p>목록·상세는 로그인 없이 볼 수 있어야 하므로 SecurityConfig에서 permitAll 처리한다.
 */
@RestController
@RequestMapping("/api/works")
public class WorkController {

    private final WorkService workService;

    public WorkController(WorkService workService) {
        this.workService = workService;
    }

    /**
     * 작품 목록 — GET /api/works?type=ANIME&genre=Action&studio=Madhouse&q=검색어&page=0&size=20
     *
     * <p>조건들은 전부 <b>AND로 함께</b> 걸린다 (하나를 골라 쓰는 게 아니다).
     *
     * <p>@PageableDefault: 클라이언트가 page·size·sort를 안 보내도 기본값이 적용된다.
     * size 상한을 두지 않으면 ?size=100000 같은 요청으로 DB를 통째로 긁어갈 수 있으니
     * application.yml에서 max-page-size도 함께 제한한다.
     */
    @GetMapping
    public ResponseEntity<PageResponse<WorkResponse>> getWorks(
            @RequestParam(required = false) WorkType type,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(required = false) String studio,
            @PageableDefault(size = 20, sort = "releaseDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(workService.getWorks(type, season, genre, keyword, studio, pageable));
    }

    /** 작품 상세 — GET /api/works/{id}. 없으면 404. */
    @GetMapping("/{id}")
    public ResponseEntity<WorkDetailResponse> getWork(@PathVariable Long id) {
        return ResponseEntity.ok(workService.getWork(id));
    }
}
