package com.haru.haruverse.community.mapper;

import com.haru.haruverse.community.dto.RecentPostResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 게시글 목록·검색 — <b>MyBatis</b>.
 *
 * <p><b>★같은 조건을 두 번 적지 않기 위해서다★</b>
 * JPQL 로는 목록 쿼리와 {@code countQuery} 에 <b>같은 where 절을 그대로 두 번</b> 써야 했다.
 * 검색 대상이 제목·본문·작성자·작품명 넷이라 조건 한 줄을 고치려면
 * 여덟 줄을 맞춰 고쳐야 했고, 한쪽만 고치면 <b>목록과 총 건수가 어긋난다</b> —
 * 화면에는 "3건"이라 떠 있는데 결과가 5개 나오는 식이다.
 *
 * <p>MyBatis 는 {@code <sql>} 조각을 두 쿼리가 함께 쓴다. 조건은 한 곳에만 있다.
 *
 * <p>덤으로 {@code <if>} 덕분에 <b>검색어가 없으면 where 절 자체가 사라진다.</b>
 * JPQL 에서는 {@code :keyword is null or ...} 를 늘 평가해야 했다.
 */
@Mapper
public interface PostSearchMapper {

    /**
     * 전체 게시판의 최근 글 — 검색어가 있으면 걸러낸다.
     *
     * @param keyword null 이면 전체 목록
     */
    List<RecentPostResponse> findRecent(@Param("keyword") String keyword,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    long countRecent(@Param("keyword") String keyword);
}
