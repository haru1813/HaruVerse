package com.haru.haruverse.external.jikan.dto;

/**
 * Jikan 단건 조회 응답 — GET /anime/{id}
 *
 * <pre>{ "data": { ...애니 1건... } }</pre>
 *
 * <p>목록 응답(JikanPageResponse)은 data가 <b>배열</b>이지만 여기는 <b>객체</b>다.
 * 같은 이름의 필드라도 형태가 다르므로 DTO를 따로 둔다.
 */
public record JikanSingleResponse(JikanAnime data) {
}
