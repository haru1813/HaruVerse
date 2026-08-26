package com.haru.haruverse.global.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이징 응답 공통 포맷.
 *
 * <p>Spring의 Page 객체를 그대로 JSON으로 내보내면 내부 구조(pageable, sort 등)가
 * 통째로 노출되고, Spring 버전이 오르면 그 구조가 바뀌어 프론트가 깨질 수 있다.
 * (실제로 Spring Boot 3.3+는 PageImpl 직렬화 시 경고를 띄운다)
 * → 우리가 계약한 필드만 담은 DTO로 감싸서 내보낸다.
 *
 * @param content       현재 페이지 데이터
 * @param page          현재 페이지 번호 (0부터)
 * @param size          페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages    전체 페이지 수
 * @param last          마지막 페이지 여부
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    /**
     * Page&lt;엔티티&gt; → PageResponse&lt;DTO&gt; 변환.
     *
     * @param page   JPA가 돌려준 페이지
     * @param mapper 엔티티 하나를 DTO 하나로 바꾸는 함수 (예: WorkResponse::from)
     */
    public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
