package com.haru.haruverse.admin.dto;

/**
 * DB 집계만 담은 값 — {@link AdminStats} 의 재료.
 *
 * <p><b>★검색 색인 수가 여기 없는 이유★</b>
 * 그 값은 Elasticsearch 에서 오고, 실패할 수도 있다(그때는 null 이다).
 * DB 집계와 성격이 완전히 달라 한 쿼리에 담을 수 없다.
 * 매퍼는 DB 만 알면 되고, 둘을 합치는 일은 서비스가 한다.
 */
public record StatsCounts(
        long works,
        long anime,
        long games,
        long titleKoFilled,
        long characters,
        long animeWithCharacters,
        long voiceActors,
        long studios,
        long members,
        long admins,
        long posts,
        long comments
) {}
