package com.haru.haruverse.admin.repository;

import com.haru.haruverse.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 감사 로그 저장소.
 *
 * <p>저장과 조회만 있다. <b>삭제 메서드를 두지 않는다</b> —
 * JpaRepository 가 delete 를 물려주지만 서비스에서 부르지 않고,
 * 컨트롤러에도 삭제 경로를 만들지 않는다.
 */
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    /** 최근 것부터 */
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
