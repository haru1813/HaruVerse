package com.haru.haruverse.character.dto;

import com.haru.haruverse.character.entity.WorkCharacter;

/**
 * 작품 상세의 등장인물 한 줄.
 *
 * <p>CharacterResponse를 감싸지 않고 <b>펼쳐서</b> 담는다.
 * 화면에서 {@code c.character.name} 대신 {@code c.name}으로 바로 쓸 수 있어
 * 카드 컴포넌트를 목록·상세 양쪽에서 그대로 재사용하기 쉽다.
 */
public record WorkCharacterResponse(
        Long id,
        String externalId,
        String name,
        String imageUrl,
        Integer favorites,
        String voiceActor,
        Long voiceActorId,
        String role   // MAIN / SUPPORTING
) {
    public static WorkCharacterResponse from(WorkCharacter wc) {
        var c = wc.getCharacter();
        var va = c.getVoiceActor();
        return new WorkCharacterResponse(
                c.getId(), c.getExternalId(), c.getName(),
                c.getImageUrl(), c.getFavorites(),
                va == null ? null : va.getName(),
                va == null ? null : va.getId(),
                wc.getRole().name());
    }
}
