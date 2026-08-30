package com.haru.haruverse.admin.dto;

import com.haru.haruverse.admin.entity.AdminAuditLog;

import java.time.LocalDateTime;

/** 감사 로그 한 줄 */
public record AuditLogResponse(
        Long id,
        String actorEmail,
        /** enum 이름 — 화면에서 색을 나눌 때 쓴다 */
        String action,
        /** 한국어 이름 — 그대로 표시한다 */
        String actionLabel,
        Long targetId,
        String summary,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AdminAuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorEmail(),
                log.getAction().name(),
                log.getAction().label(),
                log.getTargetId(),
                log.getSummary(),
                log.getCreatedAt());
    }
}
