package com.haru.haruverse.admin.controller;

import com.haru.haruverse.admin.dto.AdminStats;
import com.haru.haruverse.admin.service.AdminStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 통계 API.
 *
 * <p><b>★{@code /api/admin/**} 는 SecurityConfig 에서 ADMIN 으로 잠근다★</b>
 * 여기서 나가는 값은 회원 수·게시글 수처럼 서비스 내부 사정이다.
 * 경로 하나로 묶어 두면 앞으로 관리자 API 를 추가할 때마다
 * 권한 설정을 다시 손대지 않아도 된다 — 잠그는 걸 잊는 사고를 구조로 막는다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    private final AdminStatsService statsService;

    public AdminStatsController(AdminStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStats> stats() {
        return ResponseEntity.ok(statsService.collect());
    }
}
