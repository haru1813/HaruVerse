package com.haru.haruverse.character.entity;

/**
 * 캐릭터 데이터의 출처.
 *
 * <p>{@code AnimeCharacter.externalId}는 {@code "jikan-188175"}처럼 접두사가 붙은 문자열이다.
 * 그 규칙을 각 수집기에 문자열 리터럴로 흩어두면, 나중에 되돌려 읽는 쪽에서 실수한다
 * (작품 쪽에서 실제로 겪었다 — {@link com.haru.haruverse.work.entity.WorkSource} 참고).
 */
public enum CharacterSource {
    /** MyAnimeList 비공식 API — 애니메이션 캐릭터 */
    JIKAN("jikan-"),
    /** 붕괴: 스타레일 — MAL에 없는 게임 캐릭터 */
    STAR_RAIL("hsr-");

    private final String prefix;

    CharacterSource(String prefix) {
        this.prefix = prefix;
    }

    /** 외부 번호 → 저장할 식별자. 예) JIKAN.externalId(188175) = "jikan-188175" */
    public String externalId(Object externalKey) {
        return prefix + externalKey;
    }

    /** 이 출처에서 온 식별자인가 */
    public boolean owns(String externalId) {
        return externalId != null && externalId.startsWith(prefix);
    }
}
