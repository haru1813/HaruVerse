package com.haru.haruverse.community.entity;

import com.haru.haruverse.global.common.BaseTimeEntity;
import com.haru.haruverse.member.entity.Member;
import jakarta.persistence.*;

/**
 * 댓글과 답글.
 *
 * <p><b>★깊이는 1단계까지만★</b>
 * 답글에 다시 답글을 달 수는 없다. 무한 깊이를 허용하면 화면이 오른쪽으로
 * 계속 밀려 읽을 수 없게 되고, 조회도 재귀가 되어 쿼리 수를 예측할 수 없다.
 * 널리 쓰이는 커뮤니티들이 대부분 1단계인 것도 같은 이유다.
 * 이 규칙은 {@code PostService} 에서 강제한다 — 엔티티는 자기가 몇 번째 층인지
 * 알 수 있지만, "그래서 거부한다"는 판단은 서비스의 몫이다.
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

    /**
     * 부모 댓글 — 최상위 댓글이면 {@code null}.
     *
     * <p>★같은 테이블을 가리키는 FK 다★ 그래서 글을 지울 때
     * {@code delete from comment where post_id = ?} 한 방으로는 안 된다.
     * 답글이 부모를 참조하고 있어 순서에 따라 FK 위반이 난다.
     * 답글을 먼저 지우고 최상위를 지운다({@code CommentRepository}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_comment_parent"))
    private Comment parent;

    @Column(nullable = false, length = 1_000)
    private String content;

    protected Comment() {} // JPA용

    /** 최상위 댓글 */
    public Comment(Post post, Member member, String content) {
        this(post, member, content, null);
    }

    /** 답글 — {@code parent} 가 null 이면 최상위가 된다 */
    public Comment(Post post, Member member, String content, Comment parent) {
        this.post = post;
        this.member = member;
        this.content = content;
        this.parent = parent;
    }

    public boolean isWrittenBy(Member other) {
        return other != null && member.getId().equals(other.getId());
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public Member getMember() { return member; }
    public String getContent() { return content; }
    public Comment getParent() { return parent; }

    /** 답글인가 (= 부모가 있는가) */
    public boolean isReply() { return parent != null; }
}
