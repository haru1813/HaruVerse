package com.haru.haruverse.community.dto;

import com.haru.haruverse.community.entity.Comment;

import java.time.LocalDateTime;

/** 댓글 한 건 */
public record CommentResponse(
        Long id,
        String content,
        String authorNickname,
        Long authorId,
        boolean mine,
        LocalDateTime createdAt
) {
    public static CommentResponse of(Comment comment, boolean mine) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getMember().getNickname(),
                comment.getMember().getId(),
                mine,
                comment.getCreatedAt());
    }
}
