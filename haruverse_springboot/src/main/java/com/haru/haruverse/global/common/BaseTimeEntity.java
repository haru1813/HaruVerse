package com.haru.haruverse.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

/**
 * 생성·수정 시각을 공통으로 갖는 엔티티의 부모.
 *
 * <p>@MappedSuperclass = 이 클래스 자체는 테이블이 되지 않고,
 * 상속받은 엔티티의 테이블에 컬럼(created_at, updated_at)만 얹힌다.
 *
 * <p>Spring Data JPA Auditing(@CreatedDate)을 쓰는 방법도 있지만,
 * 그건 @EnableJpaAuditing 설정이 추가로 필요하다.
 * 여기서는 JPA 표준 콜백(@PrePersist/@PreUpdate)만으로 처리해 설정을 줄였다.
 */
@MappedSuperclass
public abstract class BaseTimeEntity {

    // updatable = false → 최초 저장 이후에는 UPDATE 문에 포함되지 않음 (생성 시각 보존)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // INSERT 직전 호출
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // UPDATE 직전 호출
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
