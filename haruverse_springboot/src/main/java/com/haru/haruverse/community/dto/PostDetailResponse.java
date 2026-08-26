package com.haru.haruverse.community.dto;

import com.haru.haruverse.community.entity.Post;

import java.time.LocalDateTime;

/** 게시글 상세 */
public record PostDetailResponse(
        Long id,
        Long workId,
        String workTitle,
        String title,
        String content,
        String authorNickname,
        Long authorId,
        int viewCount,
        long commentCount,
        long likeCount,
        /** 지금 보고 있는 사람이 추천했는지 (비로그인이면 false) */
        boolean likedByMe,
        /** 지금 보고 있는 사람이 작성자인지 — 화면에서 수정·삭제 버튼을 띄울지 판단 */
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse of(Post post, long commentCount, long likeCount,
                                        boolean likedByMe, boolean mine) {
        return new PostDetailResponse(
                post.getId(),
                post.getWork().getId(),
                post.getWork().getTitle(),
                post.getTitle(),
                post.getContent(),
                post.getMember().getNickname(),
                post.getMember().getId(),
                post.getViewCount(),
                commentCount,
                likeCount,
                likedByMe,
                mine,
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
