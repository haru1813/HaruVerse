package com.haru.haruverse.character.dto;

import com.haru.haruverse.character.entity.AnimeCharacter;
import com.haru.haruverse.character.entity.WorkCharacter;

import java.util.List;

/** 캐릭터 상세 — 기본 정보 + 출연 작품 */
public record CharacterDetailResponse(
        Long id,
        String externalId,
        String name,
        String imageUrl,
        Integer favorites,
        String voiceActor,
        Long voiceActorId,
        List<Appearance> appearances
) {
    /** 출연 작품 한 건 (작품 카드로 이동할 수 있게 workId를 함께 준다) */
    public record Appearance(Long workId, String title, String imageUrl, String role) {
        public static Appearance from(WorkCharacter wc) {
            var w = wc.getWork();
            return new Appearance(w.getId(), w.getTitle(), w.getImageUrl(), wc.getRole().name());
        }
    }

    public static CharacterDetailResponse of(AnimeCharacter c, List<WorkCharacter> links) {
        var va = c.getVoiceActor();
        return new CharacterDetailResponse(
                c.getId(), c.getExternalId(), c.getName(), c.getImageUrl(), c.getFavorites(),
                va == null ? null : va.getName(),
                va == null ? null : va.getId(),
                links.stream().map(Appearance::from).toList());
    }
}
