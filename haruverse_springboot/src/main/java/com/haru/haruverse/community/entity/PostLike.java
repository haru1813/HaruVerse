package com.haru.haruverse.community.entity;

import com.haru.haruverse.member.entity.Member;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 게시글 추천.
 *
 * <p>구조가 {@link com.haru.haruverse.favorite.entity.Favorite}과 같다 —
 * 회원과 대상을 잇고, 같은 조합이 두 번 생기지 않게 유니크 제약을 건다.
 * API도 같은 방식(PUT/DELETE 멱등)으로 맞춘다.
 */
@Entity
@Table(
        name = "post_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_like_post_member",
                columnNames = {"post_id", "member_id"}),
        indexes = @Index(name = "idx_post_like_post", columnList = "post_id")
)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", foreignKey = @ForeignKey(name = "fk_post_like_post"))
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_post_like_member"))
    private Member member;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PostLike() {} // JPA용

    public PostLike(Post post, Member member) {
        this.post = post;
        this.member = member;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public Member getMember() { return member; }
}
