package com.haru.haruverse.voiceactor.service;

import com.haru.haruverse.character.dto.CharacterResponse;
import com.haru.haruverse.character.repository.AnimeCharacterRepository;
import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.voiceactor.dto.VoiceActorDetailResponse;
import com.haru.haruverse.voiceactor.dto.VoiceActorResponse;
import com.haru.haruverse.voiceactor.entity.VoiceActor;
import com.haru.haruverse.voiceactor.repository.VoiceActorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 성우 조회.
 *
 * <p>DTO 변환은 트랜잭션 안에서 한다 (open-in-view를 꺼두었다).
 */
@Service
public class VoiceActorService {

    private final VoiceActorRepository voiceActorRepository;
    private final AnimeCharacterRepository characterRepository;

    public VoiceActorService(VoiceActorRepository voiceActorRepository,
                             AnimeCharacterRepository characterRepository) {
        this.voiceActorRepository = voiceActorRepository;
        this.characterRepository = characterRepository;
    }

    /** 성우 목록 — 맡은 캐릭터가 많은 순 */
    @Transactional(readOnly = true)
    public PageResponse<VoiceActorResponse> getVoiceActors(String keyword, Pageable pageable) {
        String k = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<VoiceActorResponse> page = voiceActorRepository.findAllWithCharacterCount(k, pageable);
        return PageResponse.of(page, r -> r); // 이미 DTO
    }

    /** 성우 상세 + 맡은 캐릭터 (인기순) */
    @Transactional(readOnly = true)
    public VoiceActorDetailResponse getVoiceActor(Long id) {
        VoiceActor v = voiceActorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("성우를 찾을 수 없습니다. id=" + id));

        List<CharacterResponse> characters = characterRepository
                .findByVoiceActorIdOrderByFavoritesDesc(id).stream()
                .map(CharacterResponse::from)
                .toList();

        return VoiceActorDetailResponse.of(v, characters);
    }
}
