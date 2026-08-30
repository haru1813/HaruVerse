package com.haru.haruverse.admin.dto;

import java.time.LocalDateTime;

/**
 * 관리자 게시글 목록의 한 줄.
 *
 * <p>운영자가 글을 지울지 판단하려면 <b>내용 일부</b>와 <b>어디에 쓴 글인지</b>가 필요하다.
 * 제목만으로는 스팸인지 정상 글인지 구분되지 않는 경우가 많다.
 *
 * <p>본문은 통째로 담지 않는다 — 목록에 10,000자짜리 글이 여럿 실리면
 * 응답이 수 MB 가 된다. 앞부분만 잘라 보낸다.
 */
public record AdminPostResponse(
        Long id,
        String title,
        /** 본문 앞부분 (자세히 보려면 서비스 화면으로 간다) */
        String excerpt,
        String authorNickname,
        Long authorId,
        /** 어느 작품 게시판에 쓴 글인지 */
        String workTitle,
        Long workId,
        int viewCount,
        long commentCount,
        LocalDateTime createdAt
) {}
