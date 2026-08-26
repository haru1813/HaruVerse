package com.haru.haruverse.community.entity;

import com.haru.haruverse.global.common.BaseTimeEntity;
import com.haru.haruverse.member.entity.Member;
import jakarta.persistence.*;

/**
 * 댓글.
 *
 * <p>대댓글(답글)은 1단계에서 만들지 않는다.
 * 부모 댓글 참조와 정렬(계층 vs 시간순)이 붙으면 화면까지 함께 복잡해진다.
 */
@Entity
@Table(
        name = "comment",
        indexes = @Index(name = "idx_comment_post_created", columnList = "post_id, created_at")
)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 글.
     *
     * <p>★글을 지우면 댓글도 함께 지워져야 한다★
     * DB 제약만으로는 안 되고, 삭제하는 쪽(PostService)에서 댓글을 먼저 지운다.
     * (cascade를 걸면 편하지만, 지워지는 범위가 코드에서 안 보여 사고가 난다)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", foreignKey = @ForeignKey(name = "fk_comment_post"))
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_comment_member"))
    private Member member;

    @Column(nullable = false, length = 1_000)
    private String content;

    protected Comment() {} // JPA용

    public Comment(Post post, Member member, String content) {
        this.post = post;
        this.member = member;
        this.content = content;
    }

    public boolean isWrittenBy(Member other) {
        return other != null && member.getId().equals(other.getId());
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public Member getMember() { return member; }
    public String getContent() { return content; }
}
