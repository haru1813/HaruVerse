package com.haru.haruverse.community.service;

import com.haru.haruverse.community.dto.*;
import com.haru.haruverse.community.entity.Comment;
import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.entity.PostLike;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostLikeRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.global.exception.ForbiddenException;
import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.repository.WorkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 게시글·댓글·추천.
 *
 * <p>인증 필터가 SecurityContext에 넣는 principal은 <b>이메일 문자열</b>이다.
 * (JwtAuthenticationFilter 참고) 그래서 서비스는 email을 받아 회원을 찾는다.
 * 비로그인 조회도 있으므로 email이 null로 들어올 수 있다.
 */
@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final WorkRepository workRepository;

    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       PostLikeRepository postLikeRepository,
                       MemberRepository memberRepository,
                       WorkRepository workRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.memberRepository = memberRepository;
        this.workRepository = workRepository;
    }

    /* ── 글 ───────────────────────────────────────────── */

    /** 작품 게시판 목록 (최신순). 비로그인도 볼 수 있다. */
    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPosts(Long workId, Pageable pageable) {
        Page<PostSummaryResponse> page = postRepository.findSummariesByWorkId(workId, pageable);
        return PageResponse.of(page, r -> r); // 이미 DTO
    }

    /**
     * 채널 목록 — 커뮤니티 첫 화면.
     *
     * <p>글이 하나라도 있는 작품만, 각 채널의 <b>최근 글</b>과 함께 돌려준다.
     *
     * <p><b>두 번 조회하는 이유</b>
     * "그룹별 최신 한 건"은 JPQL 로 한 번에 못 가져온다(서브쿼리에 limit 을 못 쓴다).
     * 네이티브 쿼리로 짜면 되지만 DB 에 묶인다.
     * → ① 작품별 글 수·최신 글 id 를 집계하고 ② 그 id 들만 한 번 더 읽는다.
     *   쿼리는 두 번이지만 카드 수와 무관하게 <b>항상 두 번</b>이다(N+1 아님).
     */
    @Transactional(readOnly = true)
    public PageResponse<ChannelResponse> getChannels(Pageable pageable) {
        Page<Object[]> stats = postRepository.findChannelStats(pageable);
        // 비었으면 여기서 끝낸다 — 아래 findByIdIn 에 빈 목록을 넘기면
        // "in ()" 이 되어 DB 에 따라 문법 오류가 난다
        if (stats.isEmpty()) {
            return PageResponse.of(stats, row -> null);
        }

        // [workId, postCount, latestPostId]
        Map<Long, Long> countByWorkId = new HashMap<>();
        List<Long> latestIds = new ArrayList<>();
        for (Object[] row : stats.getContent()) {
            Long workId = ((Number) row[0]).longValue();
            countByWorkId.put(workId, ((Number) row[1]).longValue());
            latestIds.add(((Number) row[2]).longValue());
        }

        Map<Long, Post> latestByWorkId = new HashMap<>();
        for (Post p : postRepository.findByIdInWithWorkAndMember(latestIds)) {
            latestByWorkId.put(p.getWork().getId(), p);
        }

        return PageResponse.of(stats, row -> {
            Long workId = ((Number) row[0]).longValue();
            Post latest = latestByWorkId.get(workId);
            Work work = latest.getWork(); // 최신 글이 없는 채널은 애초에 목록에 없다
            return new ChannelResponse(
                    work.getId(), work.getTitle(), work.getImageUrl(),
                    countByWorkId.get(workId),
                    latest.getId(), latest.getTitle(),
                    latest.getMember().getNickname(), latest.getCreatedAt());
        });
    }

    /** 전체 게시판의 최근 글 — 커뮤니티 첫 화면 */
    @Transactional(readOnly = true)
    public PageResponse<RecentPostResponse> getRecentPosts(Pageable pageable) {
        return PageResponse.of(postRepository.findRecentSummaries(pageable), r -> r);
    }

    /**
     * 글 상세. <b>조회수가 올라간다</b>(readOnly가 아닌 이유).
     *
     * @param email 비로그인이면 null — likedByMe·mine이 false가 된다
     */
    @Transactional
    public PostDetailResponse getPost(Long id, String email) {
        Post post = postRepository.findByIdWithMemberAndWork(id)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. id=" + id));

        post.increaseViewCount(); // 영속 상태라 커밋 때 자동 반영

        Member me = findMemberOrNull(email);
        boolean likedByMe = me != null && postLikeRepository.existsByPostIdAndMemberId(id, me.getId());
        boolean mine = post.isWrittenBy(me);

        return PostDetailResponse.of(post,
                commentRepository.countByPostId(id),
                postLikeRepository.countByPostId(id),
                likedByMe, mine);
    }

    @Transactional
    public Long createPost(Long workId, String email, PostRequest request) {
        validate(request);
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new NoSuchElementException("작품을 찾을 수 없습니다. id=" + workId));

        Post post = new Post(work, findMember(email), request.title().trim(), request.content().trim());
        return postRepository.save(post).getId();
    }

    @Transactional
    public void updatePost(Long id, String email, PostRequest request) {
        validate(request);
        Post post = findPost(id);
        requireAuthor(post, email);

        post.edit(request.title().trim(), request.content().trim());
    }

    /**
     * 글 삭제.
     *
     * <p>★삭제 순서★ 댓글·추천이 글을 참조하므로 그것들을 <b>먼저</b> 지운다.
     * 순서를 어기면 커밋 때 FK 위반으로 터진다.
     * (cascade를 걸면 코드가 짧아지지만, 무엇이 함께 지워지는지 보이지 않는다)
     */
    @Transactional
    public void deletePost(Long id, String email) {
        Post post = findPost(id);
        requireAuthor(post, email);

        commentRepository.deleteByPostId(id);
        postLikeRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    /* ── 댓글 ─────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, String email) {
        Member me = findMemberOrNull(email);
        return commentRepository.findByPostIdWithMember(postId).stream()
                .map(c -> CommentResponse.of(c, c.isWrittenBy(me)))
                .toList();
    }

    @Transactional
    public Long createComment(Long postId, String email, CommentRequest request) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new IllegalStateException("댓글 내용을 입력해주세요.");
        }
        Comment comment = new Comment(findPost(postId), findMember(email), request.content().trim());
        return commentRepository.save(comment).getId();
    }

    @Transactional
    public void deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다. id=" + commentId));

        if (!comment.isWrittenBy(findMember(email))) {
            throw new ForbiddenException("본인이 쓴 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
    }

    /* ── 추천 ─────────────────────────────────────────── */

    /**
     * 추천. <b>멱등</b> — 이미 추천한 상태로 다시 불러도 그대로 성공이다.
     *
     * <p>찜(FavoriteService)과 같은 이유로 DataIntegrityViolationException을
     * try-catch로 삼키지 않는다. INSERT가 터지면 트랜잭션이 rollback-only가 되어
     * 잡아도 커밋 때 다시 터진다 → 핸들러가 409로 응답하게 둔다.
     *
     * @return 이번 호출로 새로 추천했으면 true
     */
    @Transactional
    public boolean like(Long postId, String email) {
        Member me = findMember(email);
        if (postLikeRepository.existsByPostIdAndMemberId(postId, me.getId())) {
            return false;
        }
        postLikeRepository.save(new PostLike(findPost(postId), me));
        return true;
    }

    /** 추천 취소. 이것도 멱등 — 원래 없던 것을 지워도 성공이다. */
    @Transactional
    public boolean unlike(Long postId, String email) {
        Member me = findMember(email);
        return postLikeRepository.deleteByPostIdAndMemberId(postId, me.getId()) > 0;
    }

    /* ── 내부 ─────────────────────────────────────────── */

    private void validate(PostRequest request) {
        if (request == null
                || request.title() == null || request.title().isBlank()
                || request.content() == null || request.content().isBlank()) {
            throw new IllegalStateException("제목과 내용을 모두 입력해주세요.");
        }
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. id=" + id));
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
    }

    /** 비로그인 조회용 — 없으면 null */
    private Member findMemberOrNull(String email) {
        if (email == null || email.isBlank()) return null;
        return memberRepository.findByEmail(email).orElse(null);
    }

    /** 작성자 본인인지 검사 — 아니면 403 */
    private void requireAuthor(Post post, String email) {
        if (!post.isWrittenBy(findMember(email))) {
            throw new ForbiddenException("본인이 쓴 글만 수정·삭제할 수 있습니다.");
        }
    }
}
