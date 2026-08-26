package com.haru.haruverse.work.entity;

/**
 * 작품 데이터의 출처(수집한 외부 API).
 *
 * <p>JIKAN  — MyAnimeList 비공식 API (애니메이션)
 * <p>RAWG   — 게임 데이터베이스 API (게임)
 * <p>MANUAL — 직접 입력한 데이터 (테스트·보정용)
 *
 * <p>출처를 남겨두면 "어느 API에서 온 값인지" 추적할 수 있고,
 * 재수집 대상(예: JIKAN만 갱신)을 고를 때도 쓸 수 있다.
 *
 * <p><b>externalId 형식을 여기서 관리하는 이유</b>
 * Work.externalId는 {@code "jikan-52991"}처럼 <b>접두사가 붙은 문자열</b>이다.
 * 이 규칙이 각 Writer에 문자열 리터럴로 흩어져 있으면,
 * 나중에 이 값을 다시 숫자로 되돌리는 쪽에서 접두사를 잊고 파싱하다 터진다.
 * (실제로 캐릭터 수집에서 그렇게 실수했다 — 86건이 전부 건너뛰어졌다)
 * → 조합과 해석을 한 곳에 둔다.
 */
public enum WorkSource {
    JIKAN("jikan-"),
    RAWG("rawg-"),
    MANUAL("manual-");

    private final String prefix;

    WorkSource(String prefix) {
        this.prefix = prefix;
    }

    /** 외부 번호 → 저장할 식별자. 예) JIKAN.externalId(52991) = "jikan-52991" */
    public String externalId(Object externalKey) {
        return prefix + externalKey;
    }

    /**
     * 저장된 식별자에서 원래 번호만 꺼낸다.
     *
     * @return 형식이 맞지 않으면 null (예외를 던지지 않는다 — 수집이 한 건 때문에 멈추지 않게)
     */
    public Long extractExternalKey(String externalId) {
        if (externalId == null || !externalId.startsWith(prefix)) return null;
        try {
            return Long.parseLong(externalId.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
