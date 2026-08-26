package com.haru.haruverse.character.dto;

import com.haru.haruverse.character.entity.AnimeCharacter;

/** 캐릭터 목록용 — 카드에 필요한 것만 */
public record CharacterResponse(
        Long id,
        String externalId,
        String name,
        String imageUrl,
        Integer favorites,
        String voiceActor,
        /** 성우 상세로 이동하기 위한 id. 성우 정보가 없으면 null */
        Long voiceActorId
) {
    public static CharacterResponse from(AnimeCharacter c) {
        var va = c.getVoiceActor();
        return new CharacterResponse(
                c.getId(), c.getExternalId(), c.getName(),
                c.getImageUrl(), c.getFavorites(),
                va == null ? null : va.getName(),
                va == null ? null : va.getId());
    }
}
