package com.haru.haruverse.voiceactor.repository;

import com.haru.haruverse.voiceactor.dto.VoiceActorResponse;
import com.haru.haruverse.voiceactor.entity.VoiceActor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VoiceActorRepository extends JpaRepository<VoiceActor, Long> {

    Optional<VoiceActor> findByName(String name);

    /** 여러 이름을 한 번에 — 수집할 때 성우를 하나씩 조회하지 않도록 */
    List<VoiceActor> findByNameIn(Collection<String> names);

    /**
     * 성우 목록 + 맡은 캐릭터 수 (많이 맡은 순).
     *
     * <p>inner join이라 담당 캐릭터가 없는 성우는 나오지 않는다.
     *
     * <p>countQuery를 따로 주는 이유는 제작사 목록과 같다 —
     * group by가 있으면 자동 count가 그룹 수가 아니라 행 수를 센다.
     */
    @Query(value = """
            select new com.haru.haruverse.voiceactor.dto.VoiceActorResponse(
                       v.id, v.malId, v.name, v.imageUrl, count(c))
            from VoiceActor v join AnimeCharacter c on c.voiceActor = v
            where (:keyword is null or lower(v.name) like lower(concat('%', :keyword, '%')))
            group by v.id, v.malId, v.name, v.imageUrl
            order by count(c) desc, v.name asc
            """,
            countQuery = """
            select count(distinct v.id)
            from VoiceActor v join AnimeCharacter c on c.voiceActor = v
            where (:keyword is null or lower(v.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<VoiceActorResponse> findAllWithCharacterCount(@Param("keyword") String keyword, Pageable pageable);
}
