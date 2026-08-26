package com.haru.haruverse.character.repository;

import com.haru.haruverse.character.entity.WorkCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkCharacterRepository extends JpaRepository<WorkCharacter, Long> {

    /**
     * 작품의 등장인물 — 주역이 먼저, 그 안에서 인기순.
     *
     * <p>fetch join으로 캐릭터를 함께 읽는다. 없으면 연결 1건마다 캐릭터 쿼리가 따로 나간다(N+1).
     * 여기서는 <b>페이징을 하지 않으므로</b> fetch join을 써도 안전하다.
     * (페이징과 함께 쓰면 Hibernate가 전부 읽어 메모리에서 자르는 문제가 생긴다)
     */
    @Query("""
            select wc from WorkCharacter wc
            join fetch wc.character c
            where wc.work.id = :workId
            order by wc.role asc, c.favorites desc
            """)
    List<WorkCharacter> findByWorkIdWithCharacter(@Param("workId") Long workId);

    /** 이 작품에 이미 연결된 캐릭터 id — 재수집 시 중복 저장을 피하려고 한 번에 받는다 */
    @Query("select wc.character.id from WorkCharacter wc where wc.work.id = :workId")
    List<Long> findCharacterIdsByWorkId(@Param("workId") Long workId);

    /** 캐릭터가 출연한 작품 (캐릭터 상세용) */
    @Query("""
            select wc from WorkCharacter wc
            join fetch wc.work w
            where wc.character.id = :characterId
            order by w.releaseDate desc nulls last
            """)
    List<WorkCharacter> findByCharacterIdWithWork(@Param("characterId") Long characterId);

    long countByWorkId(Long workId);
}
