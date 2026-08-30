package com.haru.haruverse.community.repository;

import com.haru.haruverse.admin.dto.AdminCommentResponse;
import com.haru.haruverse.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 글의 댓글과 답글을 <b>한 번에</b> 읽는다 — 오래된 순(대화 흐름).
     *
     * <p>작성자를 fetch join 으로 함께 읽는다. 없으면 댓글마다 회원 조회가 한 번씩 나간다.
     *
     * <p><b>★답글을 따로 조회하지 않는다★</b>
     * 최상위 댓글을 먼저 읽고 각각의 답글을 다시 부르면 딱 N+1 이다.
     * 부모·자식을 구분하지 않고 전부 가져온 뒤, 트리는 서비스가 메모리에서 엮는다.
     * 한 글의 댓글이 수천 건이 되는 규모가 아니라 이 편이 단순하고 빠르다.
     *
     * <p>{@code left join fetch c.parent} — 트리를 엮을 때 부모 id 를 읽는다.
     * 이게 없으면 답글마다 부모를 초기화하는 쿼리가 나간다.
     */
    @Query("""
            select c from Comment c
            join fetch c.member
            left join fetch c.parent
            where c.post.id = :postId
            order by c.createdAt asc
            """)
    List<Comment> findByPostIdWithMember(@Param("postId") Long postId);

    long countByPostId(Long postId);

    /**
     * 글의 <b>답글만</b> 지운다.
     *
     * <p><b>★{@link #deleteByPostId(Long)} 보다 먼저 불러야 한다★</b>
     * comment 는 자기 테이블을 가리키는 FK(parent_id)를 갖는다.
     * 한 번에 지우려 하면 답글이 부모를 붙들고 있어 순서에 따라 FK 위반이 난다.
     * 잎(답글)부터 걷어내고 뿌리(최상위)를 지운다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from Comment c where c.post.id = :postId and c.parent is not null")
    void deleteRepliesByPostId(@Param("postId") Long postId);

    /** 글을 지울 때 호출 — 답글을 먼저 지운 뒤에 부른다 */
    @Transactional
    void deleteByPostId(Long postId);

    /**
     * 특정 댓글에 달린 답글을 모두 지운다.
     *
     * <p>댓글 하나를 지울 때 쓴다. 답글을 남겨두면 부모 없는 고아가 되고,
     * FK 때문에 부모 삭제 자체가 막힌다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from Comment c where c.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);

    /** 답글 수 — 삭제 확인 문구에 쓴다 */
    long countByParentId(Long parentId);

    /**
     * 관리자 댓글 목록 — 내용·작성자 닉네임에서 검색한다.
     *
     * <p>원글 제목을 함께 담는다. 맥락 없이 댓글만 보면 지울지 판단할 수 없다.
     */
    @Query(value = """
            select new com.haru.haruverse.admin.dto.AdminCommentResponse(
                       c.id, c.content, m.nickname, m.id, p.id, p.title,
                       c.parent.id, c.createdAt)
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
