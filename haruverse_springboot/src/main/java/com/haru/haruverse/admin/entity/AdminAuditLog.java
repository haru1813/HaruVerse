package com.haru.haruverse.admin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 관리자 행위 기록.
 *
 * <p><b>★이 프로젝트의 삭제는 되돌릴 수 없다★</b>
 * 소프트 삭제를 쓰지 않으므로 글을 지우면 댓글·추천까지 DB 에서 사라진다.
 * 나중에 "이 글이 왜 없지"를 물었을 때 답할 수 있는 유일한 흔적이 이 표다.
 *
 * <p><b>★대상 정보를 문자열로 박아 둔다★</b>
 * {@code targetId} 로 원본을 참조하지 않고 {@link #summary} 에 제목·작성자를 적어 둔다.
 * 원본이 이미 지워졌으므로 조인할 곳이 없기 때문이다.
 * FK 를 걸면 삭제 자체가 막히고, 걸지 않으면 id 만 남아 무엇이었는지 알 수 없다.
 *
 * <p><b>★지우는 API 를 만들지 않는다★</b>
 * 관리자가 자기 흔적을 지울 수 있으면 감사 로그가 아니다.
 * 조회만 열어 두고, 정리가 필요하면 DB 에서 직접 한다.
 */
@Entity
@Table(
        name = "admin_audit_log",
        indexes = @Index(name = "idx_audit_created", columnList = "created_at")
)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 실행한 관리자의 이메일.
     *
     * <p>회원을 참조(FK)하지 않는다 — 그 계정이 나중에 사라져도 기록은 남아야 한다.
     */
    @Column(name = "actor_email", nullable = false, length = 100)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    /** 대상 식별자 — 이미 지워졌을 수 있어 참조가 아니라 값으로만 둔다 */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 사람이 읽을 요약 — "제목: …, 작성자: …" 같은 형태.
     *
     * <p>대상이 지워진 뒤에도 무엇이었는지 알 수 있어야 하므로,
     * <b>지우기 전에</b> 만들어 넣는다.
     */
    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AdminAuditLog() {} // JPA용

    public AdminAuditLog(String actorEmail, AuditAction action, Long targetId, String summary) {
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetId = targetId;
        // 500자 컬럼에 넘치면 저장 자체가 실패한다. 기록을 남기는 게 목적이므로 잘라서라도 남긴다.
        this.summary = summary != null && summary.length() > 500 ? summary.substring(0, 500) : summary;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getActorEmail() { return actorEmail; }
    public AuditAction getAction() { return action; }
    public Long getTargetId() { return targetId; }
    public String getSummary() { return summary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
