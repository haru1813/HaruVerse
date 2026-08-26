package com.haru.haruverse.studio.controller;

import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.studio.dto.StudioResponse;
import com.haru.haruverse.studio.service.StudioService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 제작사 API — 비로그인도 볼 수 있다 (작품·캐릭터와 같은 취급).
 *
 * <p>제작사 '상세' 화면은 따로 두지 않았다.
 * 제작사를 고르면 {@code /api/works?studio=이름} 으로 작품을 거르는 편이
 * 장르 필터와 같은 방식이라 화면도 코드도 하나로 끝난다.
 */
@RestController
@RequestMapping("/api/studios")
public class StudioController {

    private final StudioService studioService;

    public StudioController(StudioService studioService) {
        this.studioService = studioService;
    }

    /** 제작사 목록 — GET /api/studios?q=mad&page=0&size=24 (작품 많은 순) */
    @GetMapping
    public ResponseEntity<PageResponse<StudioResponse>> getStudios(
            @RequestParam(required = false, name = "q") String keyword,
            // 정렬은 쿼리에 고정(작품 수 내림차순)이라 sort를 받지 않는다
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(studioService.getStudios(keyword, pageable));
    }
}
