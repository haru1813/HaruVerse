package com.haru.haruverse.studio.dto;

/**
 * 제작사 한 건 + 그 제작사의 작품 수.
 *
 * <p>작품 수가 없으면 목록이 그냥 이름 나열이라 볼 것이 없다.
 * JPQL 생성자 표현식(new ...StudioResponse(...))으로 집계와 함께 만든다.
 */
public record StudioResponse(
        Long id,
        String name,
        long workCount
) {}
