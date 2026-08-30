package com.haru.haruverse.admin.controller;

import com.haru.haruverse.admin.dto.AuditLogResponse;
import com.haru.haruverse.admin.repository.AdminAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 감사 로그 조회.
 *
 * <p><b>★조회만 있다★</b>
 * 삭제 경로를 만들지 않는다 — 관리자가 자기 흔적을 지울 수 있으면 감사 로그가 아니다.
 * 정리가 필요하면 DB 에서 직접 한다.
 *
 * <p>서비스 계층을 두지 않고 리포지토리를 바로 쓴다. 목록을 페이지로 읽어
 * DTO 로 옮기는 게 전부라, 사이에 낄 판단이 없다.
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {

    private final AdminAuditLogRepository repository;

    public AdminAuditController(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> list(
            @PageableDefault(size = 30) Pageable pageable) {

        return ResponseEntity.ok(
                repository.findAllByOrderByCreatedAtDesc(pageable).map(AuditLogResponse::from));
    }
}
