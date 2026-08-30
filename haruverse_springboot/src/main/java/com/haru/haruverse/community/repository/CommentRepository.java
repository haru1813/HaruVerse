package com.haru.haruverse.community.repository;

import com.haru.haruverse.admin.dto.AdminCommentResponse;
import com.haru.haruverse.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 글의 댓글 — 오래된 순(대화 흐름).
     *
     * <p>작성자를 fetch join으로 함께 읽는다. 없으면 댓글마다 회원 조회가 한 번씩 나간다.
     */
    @Query("""
            select c from Comment c
            join fetch c.member
            where c.post.id = :postId
            order by c.createdAt asc
            """)
    List<Comment> findByPostIdWithMember(@Param("postId") Long postId);

    long countByPostId(Long postId);

    /** 글을 지울 때 먼저 호출 — 댓글이 남아 있으면 FK 위반으로 삭제가 막힌다 */
    @Transactional
    void deleteByPostId(Long postId);

    /**
     * 관리자 댓글 목록 — 내용·작성자 닉네임에서 검색한다.
     *
     * <p>원글 제목을 함께 담는다. 맥락 없이 댓글만 보면 지울지 판단할 수 없다.
     */
    @Query(value = """
            select new com.haru.haruverse.admin.dto.AdminCommentResponse(
                       c.id, c.content, m.nickname, m.id, p.id, p.title, c.createdAt)
            from Comment c
            join c.member m
            join c.post p
            where (:keyword is null or :keyword = ''
                   or lower(c.content) like lower(concat('%', :keyword, '%'))
                   or lower(m.nickname) like lower(concat('%', :keyword, '%')))
            order by c.createdAt desc
            """,
            countQuery = """
            select count(c) from Comment c join c.member m
            where (:keyword is null or :keyword = ''
                   or lower(c.content) like lower(concat('%', :keyword, '%'))
                   or lower(m.nickname) like lower(concat('%', :keyword, '%')))
            """)
    Page<AdminCommentResponse> findForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
