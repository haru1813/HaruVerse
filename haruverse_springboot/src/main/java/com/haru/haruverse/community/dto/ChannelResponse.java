package com.haru.haruverse.community.dto;

import java.time.LocalDateTime;

/**
 * 커뮤니티 첫 화면의 채널 카드 한 장.
 *
 * <p><b>채널 = 작품</b>이다. 글이 하나라도 있는 작품만 카드가 된다
 * (작품 187개를 다 깔면 빈 채널이 화면을 채운다).
 *
 * <p>카드에 최근 글을 함께 보여줘야 어디가 살아 있는지 한눈에 보인다.
 */
public record ChannelResponse(
        Long workId,
        String workTitle,
        String workImageUrl,
        long postCount,

        // 가장 최근 글 — 카드 본문.
        // ★내 구독 채널 목록에서는 null 일 수 있다★ (구독은 글이 없는 채널에도 걸 수 있다)
        Long latestPostId,
        String latestPostTitle,
        String latestPostAuthor,
        LocalDateTime latestPostCreatedAt
) {}
