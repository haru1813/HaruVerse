package com.haru.haruverse.external.rawg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * RAWG API의 게임 1건.
 *
 * <p>Jikan과 마찬가지로 <b>우리가 쓸 필드만</b> 선언한다.
 * (Jackson 기본 설정이 FAIL_ON_UNKNOWN_PROPERTIES=false)
 *
 * <p>⚠️ 목록 응답과 상세 응답의 필드가 다르다.
 * <ul>
 *   <li>목록(/games)      : id·name·released·background_image·rating·metacritic·genres</li>
 *   <li>상세(/games/{id}) : 위 + description_raw·developers·publishers</li>
 * </ul>
 * 하나의 record로 둘 다 받고, 상세에만 있는 필드는 목록 수집 시 null이 된다.
 */
public record RawgGame(
        Long id,
        String slug,
        String name,
        String released,                                  // "2013-09-17"
        @JsonProperty("background_image") String backgroundImage,
        Double rating,                                    // 0.0 ~ 5.0 (RAWG 자체 평점)
        Integer metacritic,                               // 0 ~ 100 (없을 수 있음)
        List<Named> genres,
        // 묶음 플랫폼(PlayStation·Xbox·Nintendo…). 기종별 platforms 는 쓰지 않는다 —
        // "PS4·PS5·Xbox One·Xbox Series S/X…" 처럼 칩이 열 개씩 붙는다.
        @JsonProperty("parent_platforms") List<PlatformSlot> parentPlatforms,
        // ↓ 상세 응답에만 존재
        @JsonProperty("description_raw") String descriptionRaw,
        List<Named> developers,
        List<Named> publishers
) {
    public record Named(Long id, String name, String slug) {}

    /** RAWG는 플랫폼을 {"platform": {...}} 한 겹 감싸서 준다 */
    public record PlatformSlot(Named platform) {}

    /**
     * 평점을 <b>10점 만점</b>으로 변환.
     *
     * <p>★스케일 통일이 필요한 이유★
     * 애니(Jikan)는 0~10, 게임(RAWG)은 0~5다. 같은 카드 목록에 섞여 나오므로
     * 그대로 저장하면 게임이 전부 저평가된 것처럼 보인다.
     *
     * <p>metacritic(0~100)이 있으면 그쪽이 더 신뢰도가 높아 우선 사용한다.
     */
    public Double rating10() {
        if (metacritic != null && metacritic > 0) return metacritic / 10.0;
        if (rating != null && rating > 0) return rating * 2.0;
        return null;
    }

    /**
     * 플랫폼 이름들 — 순서를 유지한다(RAWG가 대표 플랫폼부터 준다).
     *
     * <p>목록·상세 응답 모두에 있으므로 재수집이 값을 지우지 않는다.
     */
    public List<String> platformNames() {
        if (parentPlatforms == null) return List.of();
        return parentPlatforms.stream()
                .map(PlatformSlot::platform)
                .filter(p -> p != null && p.name() != null && !p.name().isBlank())
                .map(p -> p.name().trim())
                .distinct()
                .toList();
    }

    /** 제작사 — 개발사 우선, 없으면 배급사 */
    public String studioName() {
        String dev = firstName(developers);
        return dev != null ? dev : firstName(publishers);
    }

    private static String firstName(List<Named> list) {
        if (list == null || list.isEmpty()) return null;
        String n = list.get(0).name();
        return (n == null || n.isBlank()) ? null : n;
    }
}
