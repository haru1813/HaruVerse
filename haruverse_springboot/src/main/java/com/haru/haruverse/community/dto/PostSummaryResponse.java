package com.haru.haruverse.community.dto;

import java.time.LocalDateTime;

/**
 * 게시판 목록의 한 줄.
 *
 * <p>댓글 수·추천 수를 <b>집계 쿼리에서 한 번에</b> 받아온다.
 * 글마다 따로 세면 20개 목록에 40번의 추가 쿼리가 나간다(N+1).
 */
public record PostSummaryResponse(
        Long id,
        String title,
        String authorNickname,
        Long authorId,
        int viewCount,
        long commentCount,
        long likeCount,
        LocalDateTime createdAt
) {}
