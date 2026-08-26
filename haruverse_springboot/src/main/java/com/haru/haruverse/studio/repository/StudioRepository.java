package com.haru.haruverse.studio.repository;

import com.haru.haruverse.studio.dto.StudioResponse;
import com.haru.haruverse.studio.entity.Studio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudioRepository extends JpaRepository<Studio, Long> {

    Optional<Studio> findByName(String name);

    /**
     * 제작사 목록 + 작품 수 (많이 만든 순).
     *
     * <p><b>inner join인 이유</b> — 작품이 0편인 제작사는 목록에 둘 이유가 없다.
     * (수집 도중 만들어졌다가 작품이 지워진 경우 등)
     *
     * <p><b>countQuery를 따로 주는 이유</b> — group by가 있으면 스프링이 자동 생성하는
     * count 쿼리가 그룹 수가 아니라 행 수를 세어 총 페이지가 어긋난다.
     */
    @Query(value = """
            select new com.haru.haruverse.studio.dto.StudioResponse(s.id, s.name, count(w))
            from Studio s join Work w on w.studio = s
            where (:keyword is null or lower(s.name) like lower(concat('%', :keyword, '%')))
            group by s.id, s.name
            order by count(w) desc, s.name asc
            """,
            countQuery = """
            select count(distinct s.id)
            from Studio s join Work w on w.studio = s
            where (:keyword is null or lower(s.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<StudioResponse> findAllWithWorkCount(@Param("keyword") String keyword, Pageable pageable);
}
