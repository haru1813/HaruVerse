package com.haru.haruverse.community.entity;

import com.haru.haruverse.global.common.BaseTimeEntity;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.work.entity.Work;
import jakarta.persistence.*;

/**
 * 게시글.
 *
 * <p><b>게시판을 따로 만들지 않은 이유</b>
 * 커뮤니티는 보통 '채널·게시판' 엔티티를 두고 사람이 만들게 한다.
 * HaruVerse에는 이미 작품이 187개 있고, 그 하나하나가 곧 게시판이다.
 * → Work를 그대로 게시판으로 쓴다. 채널 생성·관리 로직이 통째로 없어지고,
 *   도감에서 글로, 글에서 도감으로 오가는 흐름이 자연스러워진다.
 */
@Entity
@Table(
        name = "post",
        indexes = {
                // 게시판 목록의 기본 조회 — 작품별 최신순
                @Index(name = "idx_post_work_created", columnList = "work_id, created_at"),
                @Index(name = "idx_post_member", columnList = "member_id")
        }
)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어느 작품 게시판의 글인가 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", foreignKey = @ForeignKey(name = "fk_post_work"))
    private Work work;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_post_member"))
    private Member member;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 본문.
     *
     * <p>length를 크게 잡으면 H2·MariaDB 모두 TEXT 계열로 만들어진다.
     * 기본값(255)이면 조금 긴 글도 저장되지 않는다.
     */
    @Column(nullable = false, length = 10_000)
    private String content;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    protected Post() {} // JPA용

    public Post(Work work, Member member, String title, String content) {
        this.work = work;
        this.member = member;
        this.title = title;
        this.content = content;
    }

    /** 수정 — 제목·본문만 바뀐다 (작성자·작품은 그대로) */
    public void edit(String title, String content) {
        if (title != null && !title.isBlank()) this.title = title;
        if (content != null && !content.isBlank()) this.content = content;
    }

    /**
     * 조회수 증가.
     *
     * <p>⚠️ 같은 사람이 새로고침할 때마다 오른다(1단계 한계).
     * 중복을 막으려면 조회 이력을 남기거나 세션·IP로 걸러야 하는데,
     * 그 자체가 또 하나의 테이블이라 지금은 두지 않는다.
     */
    public void increaseViewCount() {
        this.viewCount++;
    }

    /** 이 글을 쓴 사람인가 — 수정·삭제 권한 판단에 쓴다 */
    public boolean isWrittenBy(Member other) {
        return other != null && member.getId().equals(other.getId());
    }

    public Long getId() { return id; }
    public Work getWork() { return work; }
    public Member getMember() { return member; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getViewCount() { return viewCount; }
}
