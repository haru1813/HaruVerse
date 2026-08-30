package com.haru.haruverse.admin.dto;

import java.time.LocalDateTime;

/**
 * 관리자 댓글 목록의 한 줄.
 *
 * <p>댓글은 본문이 짧아 통째로 담는다. 대신 <b>어느 글에 달렸는지</b>를 같이 보낸다 —
 * 맥락 없이 댓글만 보면 지울지 말지 판단할 수 없다.
 */
public record AdminCommentResponse(
        Long id,
        String content,
        String authorNickname,
        Long authorId,
        Long postId,
        String postTitle,
        LocalDateTime createdAt
) {}
