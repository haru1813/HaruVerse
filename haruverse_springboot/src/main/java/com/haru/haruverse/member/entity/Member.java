package com.haru.haruverse.member.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 회원 엔티티 — 'member' 테이블과 매핑.
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password; // 반드시 '인코딩된' 값만 저장 (평문 금지)

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Member() {} // JPA용 기본 생성자 (직접 호출 금지)

    public Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // TODO(하루): 필요 시 role(권한)·프로필 이미지 등 필드 추가

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
