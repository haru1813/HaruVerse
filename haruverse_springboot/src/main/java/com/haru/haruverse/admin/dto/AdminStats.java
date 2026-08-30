package com.haru.haruverse.admin.dto;

/**
 * 관리자 대시보드 통계.
 *
 * <p><b>★숫자를 늘어놓기만 하는 응답이 아니다★</b>
 * 관리 화면에서 필요한 건 "몇 개인가"보다 <b>"뭐가 어긋났는가"</b>다.
 * 그래서 단순 건수와 함께, 비교해야 의미가 생기는 값을 짝으로 담는다.
 * <ul>
 *   <li>{@code titleKoFilled} / {@code anime} — 한국어 제목 채움률</li>
 *   <li>{@code animeWithCharacters} / {@code anime} — 캐릭터가 비어 있는 애니가 몇 편인지</li>
 *   <li>{@code indexed} / {@code works} — <b>검색 색인이 DB와 어긋났는지</b></li>
 * </ul>
 *
 * <p>마지막 항목이 이 API 를 만든 실질적인 이유다. 예전에 색인이 48건,
 * DB 가 65건으로 벌어진 적이 있는데 <b>원인을 못 찾았다.</b>
 * 재발하면 이 화면이 먼저 알려준다.
 */
public record AdminStats(
        // ── 작품 ──
        long works,
        long anime,
        long games,

        // ── 한국어 제목 (분모는 anime) ──
        long titleKoFilled,

        // ── 콘텐츠 ──
        long characters,
        /**
         * 캐릭터가 한 명이라도 붙은 <b>애니</b> 수.
         *
         * <p>분모는 {@code anime} 다. 캐릭터는 Jikan 에서만 오므로 게임을 분모에 넣으면
         * 애초에 대상이 아닌 것까지 세어 실제보다 나쁘게 보인다.
         */
        long animeWithCharacters,
        long voiceActors,
        long studios,

        // ── 사람 ──
        long members,
        long admins,

        // ── 커뮤니티 ──
        long posts,
        long comments,

        // ── 검색 색인 ──
        /**
         * Elasticsearch 색인 문서 수.
         *
         * <p><b>null 이면 ES 에 연결하지 못한 것</b>이다. 0 과 구분해야 한다 —
         * 0 은 "연결은 됐는데 비어 있다"(재색인이 필요한 상태)이고,
         * null 은 "물어볼 수 없었다"(검색이 DB 폴백으로 동작 중)이다.
         */
        Long indexed,
        /** 색인 수와 작품 수가 다른가. ES 에 못 붙었으면 판단할 수 없으므로 false */
        boolean indexDrift
) {}
