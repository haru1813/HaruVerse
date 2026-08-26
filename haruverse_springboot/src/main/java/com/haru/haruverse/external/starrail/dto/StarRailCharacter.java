package com.haru.haruverse.external.starrail.dto;

/**
 * 붕괴: 스타레일 캐릭터 1건.
 *
 * <p>출처는 커뮤니티 리소스 저장소(StarRailRes)의 {@code index_min/en/characters.json}.
 * 응답이 배열이 아니라 <b>id를 키로 하는 객체</b>라, 호출부에서 Map으로 받는다.
 *
 * <p>실제 필드(2026-08-25 확인): id·name·tag·rarity·path·element·icon·preview·portrait 등.
 * 우리가 쓰는 것만 선언한다.
 */
public record StarRailCharacter(
        String id,        // "1001"
        String name,      // "March 7th"
        Integer rarity,   // 4 / 5
        String path,      // Knight, Rogue ...
        String element,   // Ice, Fire ...
        String icon,      // "icon/character/1001.png"
        String preview,   // "image/character_preview/1001.png"  ← 카드에 쓰기 좋은 크기
        String portrait   // "image/character_portrait/1001.png" ← 4MB, 목록에 쓰면 안 된다
) {
    /** 게임 내 플레이어 지정 이름이 들어갈 자리 */
    private static final String NICKNAME_PLACEHOLDER = "{NICKNAME}";

    /**
     * 화면에 쓸 이름.
     *
     * <p>★주인공(개척자)은 이름이 {@code "{NICKNAME}"} 으로 온다★
     * 게임에서 플레이어가 직접 정하는 이름이라 원본 데이터에는 자리표시자만 있다.
     * 그대로 저장하면 도감에 {@code {NICKNAME}} 이 열 개 뜬다(속성·성별별로 10명).
     * → 통칭인 "Trailblazer"에 속성을 붙여 구분한다.
     */
    public String displayName() {
        if (name == null || name.isBlank()) return null;
        if (name.contains(NICKNAME_PLACEHOLDER)) {
            return (element == null || element.isBlank())
                    ? "Trailblazer"
                    : "Trailblazer (" + element + ")";
        }
        return name;
    }

    /** 저장할 수 있는 최소 조건 */
    public boolean isValid() {
        return id != null && !id.isBlank() && displayName() != null;
    }
}
