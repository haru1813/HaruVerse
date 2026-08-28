package com.haru.haruverse.external.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * TMDB 검색 응답 — {@code /search/tv} · {@code /search/movie} 공용.
 *
 * <p><b>왜 두 엔드포인트를 같은 record 로 받는가</b>
 * TV와 영화는 제목 필드 이름이 다르다(TV는 {@code name}, 영화는 {@code title}).
 * 그것만 빼면 구조가 같아서, 양쪽 필드를 다 선언해두고
 * {@link Result#localizedTitle()} 이 있는 쪽을 골라 쓴다.
 * (Jackson 기본 설정이 FAIL_ON_UNKNOWN_PROPERTIES=false 라 없는 필드는 null 이 된다)
 */
public record TmdbSearchResponse(
        int page,
        List<Result> results,
        @JsonProperty("total_results") int totalResults
) {
    public record Result(
            Long id,

            // ── TV ──
            String name,
            @JsonProperty("original_name") String originalName,
            @JsonProperty("first_air_date") String firstAirDate,

            // ── 영화 ──
            String title,
            @JsonProperty("original_title") String originalTitle,
            @JsonProperty("release_date") String releaseDate,

            double popularity
    ) {
        /** 요청한 언어로 현지화된 제목 (TV면 name, 영화면 title) */
        public String localizedTitle() {
            return name != null ? name : title;
        }

        /** 원제 — 매칭 검증에 쓴다 */
        public String original() {
            return originalName != null ? originalName : originalTitle;
        }

        /** 방영·개봉 연도. 없으면 null */
        public Integer year() {
            String date = firstAirDate != null ? firstAirDate : releaseDate;
            if (date == null || date.length() < 4) return null;
            try {
                return Integer.parseInt(date.substring(0, 4));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public List<Result> safeResults() {
        return results == null ? List.of() : results;
    }
}
