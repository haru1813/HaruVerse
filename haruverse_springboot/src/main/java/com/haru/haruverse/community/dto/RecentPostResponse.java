package com.haru.haruverse.community.dto;

import java.time.LocalDateTime;

/**
 * 전체 최근 글 한 줄 — 어느 게시판(작품)의 글인지가 함께 필요하다.
 *
 * <p>작품 게시판 목록({@link PostSummaryResponse})과 달리 workId·workTitle 이 붙는다.
 * 여러 게시판의 글이 섞여 나오므로 출처를 보여줘야 한다.
 */
public record RecentPostResponse(
        Long id,
        Long workId,
        String workTitle,
        String title,
        String authorNickname,
        int viewCount,
        long commentCount,
        long likeCount,
        LocalDateTime createdAt
) {}
