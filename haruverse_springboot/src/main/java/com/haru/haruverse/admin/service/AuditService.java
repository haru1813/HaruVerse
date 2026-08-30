package com.haru.haruverse.admin.service;

import com.haru.haruverse.admin.entity.AdminAuditLog;
import com.haru.haruverse.admin.entity.AuditAction;
import com.haru.haruverse.admin.repository.AdminAuditLogRepository;
import org.springframework.stereotype.Service;

/**
 * 관리자 행위 기록.
 *
 * <p><b>★부르는 쪽과 같은 트랜잭션에서 돈다★</b>
 * {@code REQUIRES_NEW} 로 떼어내면 삭제가 실패해 롤백돼도 "삭제했다"는 기록만 남는다.
 * 로그가 거짓말을 하게 되는 것이다.
 * 같은 트랜잭션에 두면 <b>실제로 커밋된 일만</b> 기록에 남는다 —
 * 실패한 시도는 남지 않지만, 남은 기록은 전부 사실이다.
 * 감사 로그에서는 후자가 훨씬 중요하다.
 *
 * <p><b>★AOP 로 자동화하지 않았다★</b>
 * {@code @Around} 로 감싸면 코드는 짧아지지만 <b>무엇이 기록되는지 코드에서 안 보인다.</b>
 * 이 프로젝트는 cascade 대신 삭제 순서를 손으로 적는 쪽을 택했다 —
 * 무엇이 함께 일어나는지 보이게 하려는 같은 이유로, 기록도 명시적으로 부른다.
 */
@Service
public class AuditService {

    private final AdminAuditLogRepository repository;

    public AuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 기록을 남긴다.
     *
     * <p><b>★대상이 지워지기 전에 불러야 한다★</b>
     * {@code summary} 에는 제목·작성자처럼 <b>원본을 읽어야 알 수 있는</b> 값이 들어간다.
     * 지운 뒤에 부르면 읽을 곳이 없다.
     *
     * @param actorEmail 실행한 관리자 — 토큰의 subject 를 넘긴다
     * @param summary    대상이 사라진 뒤에도 무엇이었는지 알 수 있는 한 줄
     */
    public void record(String actorEmail, AuditAction action, Long targetId, String summary) {
        repository.save(new AdminAuditLog(actorEmail, action, targetId, summary));
    }
}
