package com.haru.haruverse.work.controller;

import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.work.dto.WorkDetailResponse;
import com.haru.haruverse.search.dto.SuggestionResponse;
import com.haru.haruverse.work.dto.WorkResponse;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.service.WorkService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            // ★기본 정렬을 여기서 주지 않는다★
            //   검색어가 있을 때 이 정렬이 Elasticsearch 로 넘어가면 관련도 순서를 덮어써서,
            //   "가장 잘 맞는 작품"이 아니라 "가장 최근 작품"이 위로 온다.
            //   정렬 기본값은 서비스가 '검색이 아닐 때만' 적용한다.
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(workService.getWorks(type, season, genre, keyword, studio, pageable));
    }

    /**
     * 자동완성 — GET /api/works/suggest?q=fri
     *
     * <p>★이 매핑이 {@code /{id}} 보다 <b>위에</b> 있어야 한다★
     * 아래에 두면 스프링이 "suggest" 를 id 로 해석하려다 400이 난다.
     * (구체적인 경로가 변수 경로보다 먼저 와야 한다)
     *
     * <p>비로그인도 쓴다 — {@code GET /api/works/**} 는 permitAll 이다.
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<SuggestionResponse>> suggest(
            @RequestParam(name = "q") String keyword,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(workService.suggest(keyword, size));
    }

    /** 작품 상세 — GET /api/works/{id}. 없으면 404. */
    @GetMapping("/{id}")
    public ResponseEntity<WorkDetailResponse> getWork(@PathVariable Long id) {
        return ResponseEntity.ok(workService.getWork(id));
    }
}
