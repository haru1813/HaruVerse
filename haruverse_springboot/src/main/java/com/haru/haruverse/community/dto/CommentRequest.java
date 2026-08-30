package com.haru.haruverse.community.dto;

/**
 * 댓글 작성 요청.
 *
 * @param content  내용
 * @param parentId 답글이면 부모 댓글 id, 최상위 댓글이면 {@code null}.
 *                 답글에 답글은 달 수 없다 — 깊이는 1단계까지다(PostService 가 막는다).
 */
public record CommentRequest(String content, Long parentId) {

    /**
     * 최상위 댓글 — {@code parentId} 없이.
     *
     * <p>JSON 요청에 {@code parentId} 가 빠져 있으면 잭슨이 정식 생성자에 null 을 넣으므로
     * 이 생성자는 <b>자바 코드에서 부를 때</b>를 위한 것이다.
     * 답글을 붙이면서 필드가 하나 늘었는데, 기존 호출부를 전부 고치는 대신
     * 예전 형태를 그대로 쓸 수 있게 남겨 둔다.
     */
    public CommentRequest(String content) {
        this(content, null);
    }
}
