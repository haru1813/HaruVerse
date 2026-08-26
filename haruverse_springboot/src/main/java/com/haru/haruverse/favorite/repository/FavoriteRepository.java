package com.haru.haruverse.favorite.repository;

import com.haru.haruverse.favorite.entity.Favorite;
import com.haru.haruverse.work.entity.Work;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMemberIdAndWorkId(Long memberId, Long workId);

    /** 삭제된 행 수를 돌려준다 → 0이면 "원래 찜이 아니었다" */
    @Transactional
    long deleteByMemberIdAndWorkId(Long memberId, Long workId);

    /**
     * 내가 찜한 작품 목록(최신 찜 순).
     *
     * <p>정렬을 Pageable이 아니라 쿼리에 직접 박은 이유: select 대상이 Work인데
     * 정렬 기준은 Favorite.createdAt이라, Pageable의 sort로는 표현이 애매하다.
     * (컨트롤러에서도 sort를 받지 않는다)
     *
     * <p>countQuery를 따로 주지 않으면 Spring이 select 절을 count로 바꾸려다
     * order by가 남아 문법 오류가 날 수 있다.
     */
    @Query(value = """
            select f.work from Favorite f
            where f.member.id = :memberId
            order by f.createdAt desc
            """,
            countQuery = "select count(f) from Favorite f where f.member.id = :memberId")
    Page<Work> findWorksByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 내가 찜한 작품의 id만 전부.
     *
     * <p><b>왜 목록 API에 favorited 플래그를 안 넣고 별도 엔드포인트인가</b>
     * 카드 24개를 그릴 때 각각 찜 여부가 필요한데,
     *   ① 카드마다 조회 → 요청 24번. 논외.
     *   ② WorkResponse에 favorited 필드 추가 → 작품 조회 서비스가 "누가 보고 있는지"를
     *      알아야 해서, 비로그인/로그인 분기가 도메인 안으로 파고든다.
     * → 내 찜 id를 한 번에 받아 프론트가 Set으로 들고 대조하는 게 가장 단순하다.
     * 찜이 수만 건이 되면 이 방식은 못 쓴다. 그때는 ②로 가되 별도 조회용 쿼리를 짠다.
     */
    @Query("select f.work.id from Favorite f where f.member.id = :memberId")
    List<Long> findWorkIdsByMemberId(@Param("memberId") Long memberId);
}
