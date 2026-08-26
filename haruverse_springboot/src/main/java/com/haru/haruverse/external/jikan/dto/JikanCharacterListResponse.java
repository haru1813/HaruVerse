package com.haru.haruverse.external.jikan.dto;

import java.util.List;

/**
 * {@code GET /anime/{id}/characters} 응답 껍데기.
 *
 * <p>목록 API(JikanPageResponse)와 달리 <b>pagination이 없다</b> — 한 번에 전부 내려준다.
 * (프리렌 기준 63건)
 */
public record JikanCharacterListResponse(List<JikanCharacterEntry> data) {

    /** null 대신 빈 리스트 — 호출부에서 매번 null 검사를 하지 않도록 */
    public List<JikanCharacterEntry> safeData() {
        return data == null ? List.of() : data;
    }
}
