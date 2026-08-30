package com.haru.haruverse.community.service;

import com.haru.haruverse.community.dto.*;
import com.haru.haruverse.community.entity.Comment;
import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.entity.PostLike;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostLikeRepository;
import com.haru.haruverse.community.mapper.ChannelMapper;
import com.haru.haruverse.community.mapper.PostSearchMapper;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.global.exception.ForbiddenException;
import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.repository.WorkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final PostImageService postImageService;
    private final ChannelMapper channelMapper;
    private final PostSearchMapper postSearchMapper;

    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       PostLikeRepository postLikeRepository,
                       MemberRepository memberRepository,
                       WorkRepository workRepository,
                       PostImageService postImageService,
                       ChannelMapper channelMapper,
                       PostSearchMapper postSearchMapper) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.memberRepository = memberRepository;
        this.workRepository = workRepository;
        this.postImageService = postImageService;
        this.channelMapper = channelMapper;
        this.postSearchMapper = postSearchMapper;
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
     * <p><b>★여기만 MyBatis 를 쓴다★</b>
     * 필요한 건 "작품별 최신 글 한 건 + 글 수"인데 JPQL 로는 한 번에 못 가져온다
     * (서브쿼리에 limit 을 못 쓴다). 예전에는 ① 글 수·최신 글 id 를 집계하고
     * ② 그 id 들을 다시 읽는 두 단계로 우회했다 — 쿼리 두 번에, 두 결과를
     * 코드에서 다시 엮어야 했다.
     *
     * <p>윈도우 함수를 쓰면 <b>쿼리 한 번</b>으로 끝나고 엮는 코드도 사라진다.
     * 이 프로젝트의 원칙은 <b>쓰기는 JPA, 어려운 읽기는 MyBatis</b> 다.
     */
    @Transactional(readOnly = true)
    public PageResponse<ChannelResponse> getChannels(Pageable pageable) {
        long total = channelMapper.countChannels();

        List<ChannelResponse> channels = total == 0
                ? List.of()
                : channelMapper.findChannels(pageable.getPageSize(), (int) pageable.getOffset());

        // PageResponse 는 Spring Data 의 Page 를 받는다. MyBatis 는 Page 를 만들지 않으므로
        // 조회 결과와 전체 건수로 PageImpl 을 만들어 넘긴다.
        return PageResponse.of(new PageImpl<>(channels, pageable, total), c -> c);
    }

    /**
     * 전체 게시판의 최근 글 — 검색어가 있으면 제목·본문·작성자·작품명에서 찾는다.
     *
     * <p><b>★여기도 MyBatis 다★</b>
     * JPQL 로는 목록과 총 건수에 <b>같은 where 절을 두 번</b> 써야 했다.
     * 조건이 넷이라 한 줄을 고치려면 여덟 줄을 맞춰야 했고,
     * 한쪽만 고치면 목록과 건수가 어긋난다.
     * MyBatis 는 {@code <sql>} 조각을 두 쿼리가 함께 쓴다 — 조건은 한 곳에만 있다.
     *
     * @param keyword null 이거나 공백이면 전체 목록
     */
    @Transactional(readOnly = true)
    public PageResponse<RecentPostResponse> getRecentPosts(String keyword, Pageable pageable) {
        // 빈 문자열과 null 을 하나로 맞춘다 — 매퍼의 <if> 가 둘 다 검사하지 않도록
        String q = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        long total = postSearchMapper.countRecent(q);
        List<RecentPostResponse> content = total == 0
                ? List.of()
                : postSearchMapper.findRecent(q, pageable.getPageSize(), (int) pageable.getOffset());

        return PageResponse.of(new PageImpl<>(content, pageable, total), r -> r);
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

    /** 글 삭제 — 본인 글만. 실제로 지우는 일은 {@link #purge(Post)} 가 한다 */
    @Transactional
    public void deletePost(Long id, String email) {
        Post post = findPost(id);
        requireAuthor(post, email);
        purge(post);
    }

    /**
     * 관리자 삭제 — 작성자 검증을 하지 않는다.
     *
     * <p>★권한 검사는 SecurityConfig 가 한다★
     * 이 메서드는 {@code /api/admin/**} 아래에서만 호출되고,
     * 그 경로는 {@code hasRole("ADMIN")} 으로 잠겨 있다.
     *
     * <p>★삭제 순서를 여기서 다시 쓰지 않는 이유★
     * {@link #purge(Post)} 를 함께 쓴다. 순서 지식이 두 군데로 갈라지면
     * 나중에 연관 테이블이 늘었을 때 한쪽만 고쳐 FK 위반이 난다.
     */
    @Transactional
    public void deletePostAsAdmin(Long id) {
        purge(findPost(id));
    }

    /**
     * 글과 딸린 것들을 지운다.
     *
     * <p><b>★삭제 순서★</b> 댓글·추천이 글을 참조하므로 그것들을 <b>먼저</b> 지운다.
     * 순서를 어기면 커밋 때 FK 위반으로 터진다.
     * (cascade를 걸면 코드가 짧아지지만, 무엇이 함께 지워지는지 보이지 않는다)
     *
     * <p>이 순서 지식은 <b>여기에만</b> 있다. 본인 삭제와 관리자 삭제가 함께 쓴다.
     */
    private void purge(Post post) {
        // ★답글 → 최상위 댓글 순서★ comment 는 자기 테이블을 가리키는 FK(parent_id)를
        //   갖는다. 한 번에 지우면 답글이 부모를 붙들고 있어 FK 위반이 난다.
        commentRepository.deleteRepliesByPostId(post.getId());
        commentRepository.deleteByPostId(post.getId());
        postLikeRepository.deleteByPostId(post.getId());
        // 첨부 이미지 — DB 행과 디스크 파일을 함께 정리한다
        postImageService.detachAll(post.getId());
        postRepository.delete(post);
    }

    /* ── 댓글 ─────────────────────────────────────────── */

    /**
     * 글의 댓글 — 답글이 부모 아래 중첩되어 나간다.
     *
     * <p><b>★쿼리는 한 번이다★</b> 최상위 댓글을 읽고 각각의 답글을 다시 부르면 N+1 이다.
     * 부모·자식을 구분하지 않고 전부 가져온 뒤 여기서 엮는다.
     *
     * <p>정렬은 최상위끼리 오래된 순, 답글도 부모 아래에서 오래된 순이다.
     * 쿼리가 이미 createdAt 오름차순으로 주므로 순회 순서를 그대로 쓰면 된다.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, String email) {
        Member me = findMemberOrNull(email);
        List<Comment> all = commentRepository.findByPostIdWithMember(postId);

        // 부모 id → 답글 목록
        Map<Long, List<CommentResponse>> repliesByParent = new LinkedHashMap<>();
        for (Comment c : all) {
            if (!c.isReply()) continue;
            repliesByParent
                    .computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>())
                    .add(CommentResponse.of(c, c.isWrittenBy(me)));
        }

        return all.stream()
                .filter(c -> !c.isReply())
                .map(c -> CommentResponse.of(c, c.isWrittenBy(me),
                        repliesByParent.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    /**
     * 댓글 또는 답글 작성.
     *
     * <p>{@code parentId} 가 있으면 그 댓글에 대한 답글이 된다.
     *
     * <p><b>★깊이는 1단계까지★</b> 답글에 다시 답글을 달려 하면 거부한다.
     * 무한 깊이를 허용하면 화면이 오른쪽으로 계속 밀리고 조회가 재귀가 된다.
     * 화면에서 답글의 답글 버튼을 숨기는 것만으로는 부족하다 — API 는 직접 부를 수 있다.
     */
    @Transactional
    public Long createComment(Long postId, String email, CommentRequest request) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new IllegalStateException("댓글 내용을 입력해주세요.");
        }

        Post post = findPost(postId);
        Comment parent = null;

        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "답글을 달 댓글을 찾을 수 없습니다. id=" + request.parentId()));

            if (parent.isReply()) {
                throw new IllegalStateException("답글에는 다시 답글을 달 수 없습니다.");
            }
            // 다른 글의 댓글 id 를 넣어 남의 글에 답글을 심는 걸 막는다
            if (!parent.getPost().getId().equals(post.getId())) {
                throw new IllegalStateException("이 글의 댓글이 아닙니다.");
            }
        }

        Comment comment = new Comment(post, findMember(email), request.content().trim(), parent);
        return commentRepository.save(comment).getId();
    }

    /** 관리자 댓글 삭제 — 작성자 검증 없이 지운다 (경로가 ADMIN 으로 잠겨 있다) */
    @Transactional
    public void deleteCommentAsAdmin(Long commentId) {
        purgeComment(commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다. id=" + commentId)));
    }

    @Transactional
    public void deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다. id=" + commentId));

        if (!comment.isWrittenBy(findMember(email))) {
            throw new ForbiddenException("본인이 쓴 댓글만 삭제할 수 있습니다.");
        }
        purgeComment(comment);
    }

    /**
     * 댓글과 거기 달린 답글을 지운다.
     *
     * <p>답글을 남겨두면 부모 없는 고아가 되고, FK 때문에 부모 삭제 자체가 막힌다.
     * 답글을 지울 때는 아래가 빈 호출이 된다(답글에는 자식이 없다).
     */
    private void purgeComment(Comment comment) {
        commentRepository.deleteByParentId(comment.getId());
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
