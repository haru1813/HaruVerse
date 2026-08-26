package com.haru.haruverse.community.entity;

import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.work.entity.Work;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 채널 구독 — 회원과 작품(=채널)을 잇는 연결 엔티티.
 *
 * <p><b>★찜(Favorite)과 표가 똑같은데 왜 따로 두는가★</b>
 * 둘 다 (member_id, work_id) 한 쌍이라 합치고 싶어진다. 하지만 <b>뜻이 다르다</b>.
 * <ul>
 *   <li>찜 = "이 작품이 좋다" — 도감 관점. 마이페이지 보관함에 쌓인다.</li>
 *   <li>구독 = "이 게시판의 글을 읽겠다" — 커뮤니티 관점.</li>
 * </ul>
 * 작품 자체엔 관심 없어도 정보글만 챙겨 보는 사람이 있고, 반대로 인생작이라 찜은 했지만
 * 게시판은 시끄러워서 안 보는 사람도 있다. 하나로 합치면 <b>찜을 풀 때 구독이 함께 끊긴다</b>.
 *
 * <p>표가 같다는 이유로 합치는 것은 스키마를 보고 도메인을 정하는 순서 뒤집기다.
 * 나중에 구독에만 "알림 받기" 같은 속성이 붙으면 그때 쪼개는 비용이 훨씬 크다.
 *
 * <p>유니크 제약을 거는 이유는 찜과 같다 — 서비스의 exists 검사는 "조회 → 저장" 사이에
 * 틈이 있어서(더블클릭·재시도) DB 제약이 최후의 방어선이다.
 */
@Entity
@Table(
        name = "subscription",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subscription_member_work",
                columnNames = {"member_id", "work_id"}),
        // "내 구독 채널을 전부" 가 주 조회 패턴 → member_id 기준 인덱스
        indexes = @Index(name = "idx_subscription_member", columnList = "member_id")
)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★@ManyToOne의 기본값은 EAGER★ — 명시하지 않으면 구독 하나 읽을 때마다
    //   member·work를 통째로 조인해 온다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_subscription_member"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", foreignKey = @ForeignKey(name = "fk_subscription_work"))
    private Work work;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Subscription() {} // JPA 기본 생성자

    public Subscription(Member member, Work work) {
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
