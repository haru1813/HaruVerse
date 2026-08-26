package com.haru.haruverse.character.repository;

import com.haru.haruverse.character.entity.AnimeCharacter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnimeCharacterRepository extends JpaRepository<AnimeCharacter, Long> {

    Optional<AnimeCharacter> findByExternalId(String externalId);

    /**
     * 여러 식별자를 한 번에 조회.
     *
     * <p>수집할 때 캐릭터를 하나씩 확인하면 작품당 60여 번의 쿼리가 나간다.
     * 86개 작품이면 5,000번이 넘는다. 한 번에 받아 Map으로 만들어 쓴다.
     */
    List<AnimeCharacter> findByExternalIdIn(Collection<String> externalIds);

    /** 인기순(즐겨찾기 수) — 캐릭터 도감의 기본 정렬 */
    Page<AnimeCharacter> findAllByOrderByFavoritesDesc(Pageable pageable);

    Page<AnimeCharacter> findByNameContainingIgnoreCaseOrderByFavoritesDesc(String keyword, Pageable pageable);

    /** 이 성우가 맡은 캐릭터 (인기순) — 성우 상세에서 사용 */
    List<AnimeCharacter> findByVoiceActorIdOrderByFavoritesDesc(Long voiceActorId);
}
