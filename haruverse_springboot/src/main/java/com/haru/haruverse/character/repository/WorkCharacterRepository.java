package com.haru.haruverse.character.repository;

import com.haru.haruverse.character.entity.WorkCharacter;
import com.haru.haruverse.work.entity.WorkType;
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

    /**
     * 캐릭터가 한 명이라도 연결된 작품 수 — <b>종류별로</b> 센다.
     *
     * <p>★분모와 분자를 같은 종류로 맞춰야 한다★
     * 캐릭터는 Jikan 에서 가져오므로 사실상 애니만 대상이다(게임은 스타레일이 예외).
     * 전체 작품 수를 분모로 삼으면 애초에 대상이 아닌 게임 100여 편이 섞여
     * "2% 밖에 안 채워졌다"처럼 실제보다 나쁘게 보인다.
     *
     * <p>애니 전체 수와 비교하면 '캐릭터가 비어 있는 애니'가 몇 편인지 나온다.
     * Jikan 이 504 를 자주 돌려줘 대부분이 비어 있는데, 그 상태를 눈으로 보려는 지표다.
     */
    @Query("select count(distinct wc.work.id) from WorkCharacter wc where wc.work.type = :type")
    long countDistinctWorksByType(@Param("type") WorkType type);
}
