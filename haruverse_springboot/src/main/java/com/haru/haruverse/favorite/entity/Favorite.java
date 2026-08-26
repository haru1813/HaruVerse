package com.haru.haruverse.favorite.entity;

import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.work.entity.Work;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 찜 — 회원과 작품을 잇는 연결 엔티티.
 *
 * <p><b>왜 @ManyToMany가 아니라 별도 엔티티인가</b>
 * Member에 {@code @ManyToMany Set<Work> favorites} 를 두면 코드는 짧아지지만,
 * 연결 자체에 속성을 못 붙인다. 찜은 "언제 찜했는지"가 필요하고(최신순 정렬),
 * 나중에 메모·별점 같은 게 붙을 여지도 있다. 그래서 연결을 엔티티로 승격시킨다.
 *
 * <p><b>유니크 제약을 거는 이유</b>
 * 같은 사람이 같은 작품을 두 번 찜하면 안 된다. 서비스에서 exists로 먼저 막지만,
 * 그건 "조회 → 저장" 사이에 틈이 있다(더블클릭·재시도). DB 제약이 최후의 방어선이다.
 */
@Entity
@Table(
        name = "favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_member_work",
                columnNames = {"member_id", "work_id"}),
        // "내 찜 목록을 최신순으로" 가 주 조회 패턴 → member_id 기준 인덱스
        indexes = @Index(name = "idx_favorite_member", columnList = "member_id")
)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★@ManyToOne의 기본값은 EAGER★ — 명시하지 않으면 찜 하나 읽을 때마다
    //   member·work를 통째로 조인해 온다. 찜 목록은 work만 필요하므로 LAZY로 둔다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_favorite_member"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", foreignKey = @ForeignKey(name = "fk_favorite_work"))
    private Work work;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Favorite() {} // JPA 기본 생성자

    public Favorite(Member member, Work work) {
        this.member = member;
        this.work = work;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Work getWork() { return work; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
