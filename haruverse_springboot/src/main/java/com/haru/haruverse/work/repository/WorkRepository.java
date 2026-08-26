package com.haru.haruverse.work.repository;

import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 작품 저장소.
 *
 * <p>메서드 이름만으로 쿼리가 만들어지는 게 Spring Data JPA의 쿼리 메서드 기능이다.
 * (findBy + 필드명 + 조건) → 구현 클래스를 우리가 작성할 필요가 없다.
 */
public interface WorkRepository extends JpaRepository<Work, Long>, JpaSpecificationExecutor<Work> {

    // ↓ 아래 파생 메서드들은 단일 조건 조회용으로 남아 있다.
    //   목록 화면의 복합 필터는 WorkSpecs(Specification)를 쓴다 — 조건 조합이 5개라
    //   파생 메서드로는 다 만들 수 없기 때문이다.

    // 종류별 목록 — GET /api/works?type=ANIME
    Page<Work> findByType(WorkType type, Pageable pageable);

    // 분기별 목록 — GET /api/works?season=2026-spring
    Page<Work> findBySeason(String season, Pageable pageable);

    // 종류 + 분기 동시 필터
    Page<Work> findByTypeAndSeason(WorkType type, String season, Pageable pageable);

    // 제목 부분 일치 (대소문자 무시). 임시 검색용 —
    // 나중에 Elasticsearch로 옮길 자리다. (설계문서 ④ search 섹션)
    Page<Work> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    // 장르명으로 필터 — 언더스코어(_)는 "연관 필드를 파고든다"는 뜻.
    // genres_Name → work.genres 컬렉션의 Genre.name 을 조건으로 건다 (자동 조인)
    Page<Work> findByGenres_NameIgnoreCase(String genreName, Pageable pageable);

    Page<Work> findByTypeAndGenres_NameIgnoreCase(WorkType type, String genreName, Pageable pageable);

    // 외부 API 재수집 시 "이미 있는 작품인가?" 판단 (external_id는 UNIQUE)
    Optional<Work> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    /**
     * 특정 출처의 작품들 (캐릭터 수집 대상 고르기용).
     *
     * <p>캐릭터는 Jikan에만 있으므로 source=JIKAN인 작품만 훑는다.
     * Pageable로 개수를 제한해 한 번에 다 긁지 않게 한다.
     */
    Page<Work> findBySource(WorkSource source, Pageable pageable);
}
