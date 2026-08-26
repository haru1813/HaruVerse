package com.haru.haruverse.voiceactor.dto;

/**
 * 성우 한 명 + 맡은 캐릭터 수.
 *
 * <p>캐릭터 수가 없으면 목록이 이름 나열이라 볼 것이 없다.
 * (제작사 목록에서 작품 수를 함께 준 것과 같은 이유)
 */
public record VoiceActorResponse(
        Long id,
        Long malId,
        String name,
        String imageUrl,
        long characterCount
) {}
