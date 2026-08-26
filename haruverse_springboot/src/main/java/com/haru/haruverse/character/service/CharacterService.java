package com.haru.haruverse.character.service;

import com.haru.haruverse.character.dto.CharacterDetailResponse;
import com.haru.haruverse.character.dto.CharacterResponse;
import com.haru.haruverse.character.dto.WorkCharacterResponse;
import com.haru.haruverse.character.entity.AnimeCharacter;
import com.haru.haruverse.character.entity.WorkCharacter;
import com.haru.haruverse.character.repository.AnimeCharacterRepository;
import com.haru.haruverse.character.repository.WorkCharacterRepository;
import com.haru.haruverse.global.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 캐릭터 조회.
 *
 * <p>WorkService와 같은 원칙 — <b>DTO 변환을 트랜잭션 안에서</b> 한다.
 * 컨트롤러에서 변환하면 지연 로딩이 트랜잭션 밖에서 일어나 터진다
 * (open-in-view를 꺼두었다).
 */
@Service
public class CharacterService {

    private final AnimeCharacterRepository characterRepository;
    private final WorkCharacterRepository workCharacterRepository;

    public CharacterService(AnimeCharacterRepository characterRepository,
                            WorkCharacterRepository workCharacterRepository) {
        this.characterRepository = characterRepository;
        this.workCharacterRepository = workCharacterRepository;
    }

    /** 캐릭터 목록 — 인기순(즐겨찾기 수). 검색어가 있으면 이름으로 거른다. */
    @Transactional(readOnly = true)
    public PageResponse<CharacterResponse> getCharacters(String keyword, Pageable pageable) {
        Page<AnimeCharacter> page = (keyword != null && !keyword.isBlank())
                ? characterRepository.findByNameContainingIgnoreCaseOrderByFavoritesDesc(keyword.trim(), pageable)
                : characterRepository.findAllByOrderByFavoritesDesc(pageable);

        return PageResponse.of(page, CharacterResponse::from);
    }

    /** 캐릭터 상세 + 출연 작품 */
    @Transactional(readOnly = true)
    public CharacterDetailResponse getCharacter(Long id) {
        AnimeCharacter c = characterRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다. id=" + id));

        List<WorkCharacter> links = workCharacterRepository.findByCharacterIdWithWork(id);
        return CharacterDetailResponse.of(c, links);
    }

    /** 작품의 등장인물 — 주역 먼저, 그 안에서 인기순 */
    @Transactional(readOnly = true)
    public List<WorkCharacterResponse> getCharactersOfWork(Long workId) {
        return workCharacterRepository.findByWorkIdWithCharacter(workId).stream()
                .map(WorkCharacterResponse::from)
                .toList();
    }
}
