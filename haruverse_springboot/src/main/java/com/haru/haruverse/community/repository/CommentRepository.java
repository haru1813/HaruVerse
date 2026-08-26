package com.haru.haruverse.community.repository;

import com.haru.haruverse.community.entity.Comment;
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
}
