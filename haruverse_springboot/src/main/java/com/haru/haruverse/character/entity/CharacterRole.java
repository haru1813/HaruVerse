package com.haru.haruverse.character.entity;

/**
 * 작품 속 캐릭터의 비중.
 *
 * <p>Jikan은 "Main" / "Supporting" 문자열로 준다.
 * 문자열을 그대로 저장하면 대소문자·오타로 조건이 어긋나므로 enum으로 좁힌다.
 */
public enum CharacterRole {
    MAIN,
    SUPPORTING;

    /** Jikan의 role 문자열 → enum. 모르는 값이면 SUPPORTING으로 둔다(수집이 멈추지 않게). */
    public static CharacterRole from(String raw) {
        if (raw == null) return SUPPORTING;
        return "Main".equalsIgnoreCase(raw.trim()) ? MAIN : SUPPORTING;
    }
}
