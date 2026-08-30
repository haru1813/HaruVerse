package com.haru.haruverse.community.repository;

import com.haru.haruverse.admin.dto.AdminPostResponse;
import com.haru.haruverse.community.dto.PostSummaryResponse;
import com.haru.haruverse.community.dto.RecentPostResponse;
import com.haru.haruverse.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 작품 게시판 목록 — 최신순, 댓글 수·추천 수 포함.
     *
     * <p><b>★left join + group by를 쓰는 이유★</b>
     * 글마다 댓글 수를 따로 세면 20개 목록에 40번의 추가 쿼리가 나간다(N+1).
     * 한 번에 집계해서 DTO로 받는다.
     *
     * <p><b>left join이어야 한다</b> — inner join이면 댓글이 하나도 없는 글이
     * 목록에서 통째로 사라진다. 새로 쓴 글이 안 보이는 버그가 여기서 난다.
     *
     * <p><b>countQuery를 따로 주는 이유</b> — group by가 있으면 자동 생성된 count가
     * 그룹 수가 아니라 행 수를 센다 (제작사·성우 목록에서 겪은 것과 같은 문제).
     */
    @Query(value = """
            select new com.haru.haruverse.community.dto.PostSummaryResponse(
                       p.id, p.title, m.nickname, m.id, p.viewCount,
                       count(distinct c.id), count(distinct l.id), p.createdAt)
            from Post p
            join p.member m
            left join Comment c on c.post = p
            left join PostLike l on l.post = p
            where p.work.id = :workId
            group by p.id, p.title, m.nickname, m.id, p.viewCount, p.createdAt
            order by p.createdAt desc
            """,
            countQuery = "select count(p) from Post p where p.work.id = :workId")
    Page<PostSummaryResponse> findSummariesByWorkId(@Param("workId") Long workId, Pageable pageable);

    /**
     * 상세 조회 — 작성자·작품을 함께 읽는다.
     *
     * <p>fetch join이 없으면 DTO를 만들 때 작성자 닉네임·작품 제목에서
     * 각각 추가 쿼리가 나간다. 페이징이 없으므로 fetch join을 써도 안전하다.
     */
    @Query("""
            select p from Post p
            join fetch p.member
            join fetch p.work
            where p.id = :id
            """)
    Optional<Post> findByIdWithMemberAndWork(@Param("id") Long id);

    // 최근 글 목록·검색은 MyBatis 로 옮겼다(PostSearchMapper).
    //   JPQL 로는 같은 where 절을 목록과 countQuery 에 두 번 써야 했고,
    //   한쪽만 고치면 목록과 총 건수가 어긋났다.

    /**
     * 채널(작품)별 글 수와 <b>최신 글 id</b> — 커뮤니티 첫 화면 1단계.
     *
     * <p>반환: [workId, postCount, latestPostId]
     *
     * <p>★max(createdAt) 이 아니라 max(id) 를 쓰는 이유★
     * 같은 시각에 쓰인 글이 둘이면 createdAt 으로는 최신 한 건을 특정할 수 없어
     * 카드가 중복된다. id 는 auto increment 라 최신 글이 곧 최대값이다.
     *
     * <p>글이 있는 작품만 나온다(Post 를 기준으로 group by 하므로 빈 채널은 애초에 없다).
     */
    @Query(value = """
            select p.work.id, count(p), max(p.id)
            from Post p
            group by p.work.id
            order by max(p.id) desc
            """,
            countQuery = "select count(distinct p.work.id) from Post p")
    Page<Object[]> findChannelStats(Pageable pageable);

    /**
     * 최신 글들을 작품·작성자와 함께 — 커뮤니티 첫 화면 2단계.
     *
     * <p>fetch join 이 없으면 카드마다 작품 제목·작성자 조회가 따로 나간다(N+1).
     * id 목록으로 조회하므로 페이징이 없어 fetch join 이 안전하다.
     */
    @Query("""
            select p from Post p
            join fetch p.work
            join fetch p.member
            where p.id in :ids
            """)
    List<Post> findByIdInWithWorkAndMember(@Param("ids") Collection<Long> ids);

    long countByWorkId(Long workId);

    /**
     * 지정한 작품들의 글 수·최신 글 id — 내 구독 채널 카드용.
     *
     * <p>findChannelStats 와 같은 집계지만 <b>대상이 정해져 있다</b>(내가 구독한 작품).
     * 페이징이 없으므로 Object[] 목록을 그대로 돌려준다. [workId, count, maxId]
     *
     * <p>★글이 하나도 없는 채널은 여기 안 나온다★ — Post 기준 group by 이기 때문이다.
     * 구독은 글이 없는 채널에도 걸 수 있으므로, 빠진 채널은 서비스가 글 0개로 채운다.
     */
    @Query("""
            select p.work.id, count(p), max(p.id)
            from Post p
            where p.work.id in :workIds
            group by p.work.id
            """)
    List<Object[]> findChannelStatsByWorkIds(@Param("workIds") Collection<Long> workIds);

    // 관리자 목록은 MyBatis 로 옮겼다(AdminCommunityMapper) — where 절 중복 제거.

}
