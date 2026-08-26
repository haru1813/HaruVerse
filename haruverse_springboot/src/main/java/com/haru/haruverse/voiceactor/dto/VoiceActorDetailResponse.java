package com.haru.haruverse.voiceactor.dto;

import com.haru.haruverse.character.dto.CharacterResponse;
import com.haru.haruverse.voiceactor.entity.VoiceActor;

import java.util.List;

/** 성우 상세 — 기본 정보 + 맡은 캐릭터 목록 */
public record VoiceActorDetailResponse(
        Long id,
        Long malId,
        String name,
        String imageUrl,
        List<CharacterResponse> characters
) {
    public static VoiceActorDetailResponse of(VoiceActor v, List<CharacterResponse> characters) {
        return new VoiceActorDetailResponse(v.getId(), v.getMalId(), v.getName(), v.getImageUrl(), characters);
    }
}
