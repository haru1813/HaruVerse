package com.haru.haruverse.external.jikan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jikan API 목록 응답의 껍데기.
 *
 * <pre>
 * { "pagination": { "last_visible_page": 15365, "has_next_page": true, ... },
 *   "data": [ { ...애니... }, ... ] }
 * </pre>
 */
public record JikanPageResponse(
        Pagination pagination,
        List<JikanAnime> data
) {
    public record Pagination(
            @JsonProperty("last_visible_page") Integer lastVisiblePage,
            @JsonProperty("has_next_page") Boolean hasNextPage,
            @JsonProperty("current_page") Integer currentPage
    ) {}

    /** data가 null로 오는 경우(에러 응답 등)를 대비 — 호출부에서 null 체크를 반복하지 않도록 */
    public List<JikanAnime> safeData() {
        return data == null ? List.of() : data;
    }

    public boolean hasNext() {
        return pagination != null && Boolean.TRUE.equals(pagination.hasNextPage());
    }
}
