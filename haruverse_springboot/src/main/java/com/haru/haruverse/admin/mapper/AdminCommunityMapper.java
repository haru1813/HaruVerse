package com.haru.haruverse.admin.mapper;

import com.haru.haruverse.admin.dto.AdminCommentResponse;
import com.haru.haruverse.admin.dto.AdminPostResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 관리자 커뮤니티 목록 — <b>MyBatis</b>.
 *
 * <p>게시글 검색과 같은 이유다 — JPQL 로는 같은 where 절을 목록과 {@code countQuery} 에
 * 두 번 써야 했고, 한쪽만 고치면 목록과 총 건수가 어긋난다.
 * {@code <sql>} 조각으로 한 곳에 모은다.
 */
@Mapper
public interface AdminCommunityMapper {

    List<AdminPostResponse> findPosts(@Param("keyword") String keyword,
                                      @Param("limit") int limit,
                                      @Param("offset") int offset);

    long countPosts(@Param("keyword") String keyword);

    List<AdminCommentResponse> findComments(@Param("keyword") String keyword,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);

    long countComments(@Param("keyword") String keyword);
}
