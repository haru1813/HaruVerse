package com.haru.haruverse.external.jikan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jikan API의 애니메이션 1건.
 *
 * <p>실제 응답에는 36개 넘는 필드가 오지만 <b>우리가 쓸 것만</b> 선언했다.
 * Spring Boot의 Jackson 기본 설정이 FAIL_ON_UNKNOWN_PROPERTIES=false 라서
 * 선언하지 않은 필드는 조용히 무시된다.
 * → 외부 API가 필드를 추가해도 우리 코드가 깨지지 않는다.
 *
 * <p>JSON은 snake_case, 자바는 camelCase라 @JsonProperty로 이름을 이어준다.
 */
public record JikanAnime(
        @JsonProperty("mal_id") Long malId,
        String title,
        @JsonProperty("title_english") String titleEnglish,
        String type,           // TV / Movie / OVA ... (우리 WorkType은 전부 ANIME)
        Double score,          // 평점 (없으면 null)
        String season,         // spring / summer / fall / winter (없을 수 있음)
        Integer year,          // 방영 연도 (없을 수 있음)
        String synopsis,
        Images images,
        Aired aired,
        List<Named> studios,
        List<Named> genres
) {
    /** images.jpg.large_image_url 만 필요 */
    public record Images(Jpg jpg) {
        public record Jpg(
                @JsonProperty("image_url") String imageUrl,
                @JsonProperty("large_image_url") String largeImageUrl
        ) {}
    }

    /** aired.from = "2023-09-29T00:00:00+00:00" (ISO 8601 offset) */
    public record Aired(String from, String to) {}

    /** studios·genres는 {mal_id, type, name, url} 형태 → 이름만 쓴다 */
    public record Named(@JsonProperty("mal_id") Long malId, String name) {}

    /* ── 편의 메서드 — 매핑 로직을 DTO 안에 두어 서비스를 얇게 유지 ── */

    /** 포스터 URL (큰 이미지 우선, 없으면 기본 이미지) */
    public String posterUrl() {
        if (images == null || images.jpg() == null) return null;
        return images.jpg().largeImageUrl() != null
                ? images.jpg().largeImageUrl()
                : images.jpg().imageUrl();
    }

    /** ERD의 season 형식으로 조합 — "2023-fall". 둘 중 하나라도 없으면 null */
    public String seasonKey() {
        if (season == null || year == null) return null;
        return year + "-" + season;
    }
}
