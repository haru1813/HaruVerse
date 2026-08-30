package com.haru.haruverse.community.entity;

import com.haru.haruverse.global.common.BaseTimeEntity;
import jakarta.persistence.*;

/**
 * 게시글 첨부 이미지.
 *
 * <p><b>★본문에 끼워 넣지 않고 따로 붙인다★</b>
 * 본문은 순수 텍스트다(작성 폼이 그냥 textarea 다). 이미지를 본문 안에 넣으려면
 * 마크다운이나 HTML 렌더링이 필요하고, 그 순간 <b>XSS 방어</b>가 따라온다 —
 * 사용자가 {@code <script>} 나 {@code onerror} 를 쓸 수 있게 되기 때문이다.
 * 첨부 목록으로 두면 본문은 그대로 텍스트로 남고, 파일 업로드의 쟁점만 다루면 된다.
 *
 * <p><b>★원본 파일명을 저장 이름으로 쓰지 않는다★</b>
 * {@link #storedName} 은 서버가 만든 UUID 다. 원본 이름을 그대로 쓰면
 * {@code ../../etc/passwd} 같은 경로 탐색이 열리고, 같은 이름끼리 덮어쓴다.
 * {@link #originalName} 은 화면에 보여주기 위해서만 갖고 있는다.
 */
@Entity
@Table(
        name = "post_image",
        indexes = @Index(name = "idx_post_image_post", columnList = "post_id")
)
public class PostImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 글.
     *
     * <p>글을 지울 때 이 행들을 먼저 지운다(FK). 댓글·추천과 같은 이유이고,
     * 순서는 {@code PostService.purge} 한 곳에만 적혀 있다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", foreignKey = @ForeignKey(name = "fk_post_image_post"))
    private Post post;

    /** 서버가 만든 파일명 — UUID + 확장자. 디스크에 이 이름으로 놓인다 */
    @Column(name = "stored_name", nullable = false, unique = true, length = 100)
    private String storedName;

    /** 올린 사람이 쓰던 이름 — 표시용일 뿐 파일 접근에 쓰지 않는다 */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 60)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    protected PostImage() {} // JPA용

    public PostImage(Post post, String storedName, String originalName,
                     String contentType, long byteSize) {
        this.post = post;
        this.storedName = storedName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.byteSize = byteSize;
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public String getStoredName() { return storedName; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getByteSize() { return byteSize; }
}
