package com.haru.haruverse.external.jikan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jikan {@code GET /anime/{id}/characters} 응답의 항목 1건.
 *
 * <p>응답 형태(2026-08-25 실제 확인):
 * <pre>
 * { "character": { "mal_id":188176, "name":"Fern", "images":{"jpg":{"image_url":"..."}} },
 *   "role": "Main",
 *   "favorites": 5877,
 *   "voice_actors": [ { "person":{"mal_id":47097,"name":"Ichinose, Kana"}, "language":"Japanese" }, ... ] }
 * </pre>
 *
 * <p>⚠️ favorites는 character 안이 아니라 <b>바깥</b>에 있다.
 * <p>⚠️ voice_actors에는 10개 언어가 들어온다 (Spanish·Portuguese·Japanese·English·French …).
 */
public record JikanCharacterEntry(
        Character character,
        String role,               // "Main" / "Supporting"
        Integer favorites,
        @JsonProperty("voice_actors") List<VoiceActor> voiceActors
) {
    public record Character(
            @JsonProperty("mal_id") Long malId,
            String name,
            Images images
    ) {
        public record Images(Jpg jpg) {
            public record Jpg(@JsonProperty("image_url") String imageUrl) {}
        }
    }

    public record VoiceActor(Person person, String language) {
        // 성우도 캐릭터와 같은 형태로 식별자·이미지를 준다
        public record Person(
                @JsonProperty("mal_id") Long malId,
                String name,
                Images images
        ) {
            public record Images(Jpg jpg) {
                public record Jpg(@JsonProperty("image_url") String imageUrl) {}
            }

            public String imageUrl() {
                if (images == null || images.jpg() == null) return null;
                return images.jpg().imageUrl();
            }
        }
    }

    /* ── 편의 메서드 — 매핑 로직을 DTO에 두어 수집 서비스를 얇게 유지 ── */

    public Long malId() {
        return character == null ? null : character.malId();
    }

    public String name() {
        return character == null ? null : character.name();
    }

    public String imageUrl() {
        if (character == null || character.images() == null || character.images().jpg() == null) return null;
        return character.images().jpg().imageUrl();
    }

    /**
     * 일본어 성우.
     *
     * <p>10개 언어가 전부 오지만 우리가 쓰는 건 일본어뿐이다.
     * 같은 언어로 두 명이 오는 경우도 있어(Stark) 첫 번째를 쓴다.
     * 없으면 null (오래된 작품·단역에는 성우 정보가 비어 있다).
     */
    public VoiceActor.Person japaneseVoiceActorPerson() {
        if (voiceActors == null) return null;
        return voiceActors.stream()
                .filter(va -> "Japanese".equalsIgnoreCase(va.language()))
                .map(VoiceActor::person)
                .filter(p -> p != null && p.name() != null && !p.name().isBlank())
                .findFirst()
                .orElse(null);
    }

    /** 일본어 성우 이름만 (기존 호출부 호환) */
    public String japaneseVoiceActor() {
        VoiceActor.Person p = japaneseVoiceActorPerson();
        return p == null ? null : p.name();
    }

    /** 저장할 수 있는 최소 조건 — 식별자와 이름이 있어야 한다 */
    public boolean isValid() {
        return malId() != null && name() != null && !name().isBlank();
    }
}
