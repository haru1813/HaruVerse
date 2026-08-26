package com.haru.haruverse.community;

import com.haru.haruverse.community.dto.CommentRequest;
import com.haru.haruverse.community.dto.PostRequest;
import com.haru.haruverse.community.dto.PostSummaryResponse;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostLikeRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.service.PostService;
import com.haru.haruverse.global.exception.ForbiddenException;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 커뮤니티 — 글·댓글·추천.
 *
 * <p>@Transactional을 붙이지 않는다 — 붙이면 테스트가 한 트랜잭션으로 묶여
 * 서비스의 트랜잭션 경계와 실제 커밋에서만 드러나는 문제를 놓친다.
 */
@SpringBootTest
class PostServiceTest {

    @Autowired PostService postService;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired PostLikeRepository postLikeRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    private static final String AUTHOR = "post-author@haru.test";
    private static final String OTHER = "post-other@haru.test";

    private Long workId;

    @BeforeEach
    void setUp() {
        cleanUp();

        memberRepository.findByEmail(AUTHOR).orElseGet(
                () -> memberRepository.save(new Member(AUTHOR, "encoded", "글쓴이")));
        memberRepository.findByEmail(OTHER).orElseGet(
                () -> memberRepository.save(new Member(OTHER, "encoded", "다른사람")));

        workId = workRepository.save(new Work("게시판테스트 작품", WorkType.ANIME, WorkSource.MANUAL)).getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    /** ★삭제 순서★ 추천·댓글 → 글 → 작품 (참조하는 쪽부터) */
    private void cleanUp() {
        workRepository.findAll().stream()
                .filter(w -> w.getTitle().startsWith("게시판테스트"))
                .forEach(w -> {
                    postRepository.findSummariesByWorkId(w.getId(), PageRequest.of(0, 500))
                            .forEach(p -> {
                                postLikeRepository.deleteByPostId(p.id());
                                commentRepository.deleteByPostId(p.id());
                                postRepository.deleteById(p.id());
                            });
                    workRepository.delete(w);
                });
    }

    private List<PostSummaryResponse> list() {
        return postService.getPosts(workId, PageRequest.of(0, 20)).content();
    }

    /* ── 글 ───────────────────────────────────────────── */

    @Test
    @DisplayName("글을 쓰면 그 작품 게시판에 나온다")
    void createAndList() {
        postService.createPost(workId, AUTHOR, new PostRequest("첫 글", "내용입니다"));

        assertThat(list()).extracting(PostSummaryResponse::title).containsExactly("첫 글");
        assertThat(list().get(0).authorNickname()).isEqualTo("글쓴이");
    }

    @Test
    @DisplayName("★댓글이 없는 글도 목록에 나온다★ (left join이 아니면 사라진다)")
    void postWithoutCommentsAppears() {
        Long withComment = postService.createPost(workId, AUTHOR, new PostRequest("댓글 있는 글", "내용"));
        postService.createPost(workId, AUTHOR, new PostRequest("댓글 없는 글", "내용"));
        postService.createComment(withComment, OTHER, new CommentRequest("댓글"));

        // inner join으로 짰다면 '댓글 없는 글'이 통째로 빠진다 — 새 글이 안 보이는 버그
        assertThat(list()).extracting(PostSummaryResponse::title)
                .containsExactlyInAnyOrder("댓글 있는 글", "댓글 없는 글");
    }

    @Test
    @DisplayName("목록에 댓글 수·추천 수가 함께 집계된다")
    void listCarriesCounts() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("집계 글", "내용"));
        postService.createComment(postId, AUTHOR, new CommentRequest("댓글1"));
        postService.createComment(postId, OTHER, new CommentRequest("댓글2"));
        postService.like(postId, OTHER);

        PostSummaryResponse row = list().get(0);
        assertThat(row.commentCount()).isEqualTo(2);
        assertThat(row.likeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("★중복 집계 주의★ 댓글 2 + 추천 2여도 각각 2로 나온다 (곱해지지 않는다)")
    void countsAreNotMultiplied() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("중복 집계", "내용"));
        postService.createComment(postId, AUTHOR, new CommentRequest("댓글1"));
        postService.createComment(postId, OTHER, new CommentRequest("댓글2"));
        postService.like(postId, AUTHOR);
        postService.like(postId, OTHER);

        // join이 둘이라 distinct가 없으면 2×2=4로 부풀려진다
        PostSummaryResponse row = list().get(0);
        assertThat(row.commentCount()).isEqualTo(2);
        assertThat(row.likeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("상세를 열면 조회수가 오른다")
    void viewCountIncreases() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("조회수", "내용"));

        assertThat(postService.getPost(postId, null).viewCount()).isEqualTo(1);
        assertThat(postService.getPost(postId, null).viewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("비로그인도 글을 볼 수 있다 (likedByMe·mine은 false)")
    void anonymousCanRead() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("공개 글", "내용"));

        var detail = postService.getPost(postId, null);
        assertThat(detail.title()).isEqualTo("공개 글");
        assertThat(detail.likedByMe()).isFalse();
        assertThat(detail.mine()).isFalse();
    }

    @Test
    @DisplayName("작성자에게는 mine=true")
    void authorSeesMine() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("내 글", "내용"));

        assertThat(postService.getPost(postId, AUTHOR).mine()).isTrue();
        assertThat(postService.getPost(postId, OTHER).mine()).isFalse();
    }

    @Test
    @DisplayName("★남의 글은 수정·삭제할 수 없다★ (403)")
    void cannotEditOthersPost() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("내 글", "내용"));

        assertThatThrownBy(() -> postService.updatePost(postId, OTHER, new PostRequest("탈취", "탈취")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> postService.deletePost(postId, OTHER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("제목이나 내용이 비면 저장하지 않는다")
    void rejectsEmpty() {
        assertThatThrownBy(() -> postService.createPost(workId, AUTHOR, new PostRequest("", "내용")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> postService.createPost(workId, AUTHOR, new PostRequest("제목", "   ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("★글을 지우면 댓글·추천도 함께 지워진다★ (남으면 FK 위반)")
    void deleteCascades() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("지울 글", "내용"));
        postService.createComment(postId, OTHER, new CommentRequest("댓글"));
        postService.like(postId, OTHER);

        postService.deletePost(postId, AUTHOR);

        assertThat(postRepository.findById(postId)).isEmpty();
        assertThat(commentRepository.countByPostId(postId)).isZero();
        assertThat(postLikeRepository.countByPostId(postId)).isZero();
    }

    /* ── 댓글 ─────────────────────────────────────────── */

    @Test
    @DisplayName("댓글은 오래된 순으로 나온다 (대화 흐름)")
    void commentsInOrder() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("글", "내용"));
        postService.createComment(postId, AUTHOR, new CommentRequest("첫째"));
        postService.createComment(postId, OTHER, new CommentRequest("둘째"));

        assertThat(postService.getComments(postId, null))
                .extracting("content").containsExactly("첫째", "둘째");
    }

    @Test
    @DisplayName("남의 댓글은 삭제할 수 없다 (403)")
    void cannotDeleteOthersComment() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("글", "내용"));
        Long commentId = postService.createComment(postId, AUTHOR, new CommentRequest("내 댓글"));

        assertThatThrownBy(() -> postService.deleteComment(commentId, OTHER))
                .isInstanceOf(ForbiddenException.class);
    }

    /* ── 추천 ─────────────────────────────────────────── */

    @Test
    @DisplayName("★멱등★ 같은 글을 두 번 추천해도 1건")
    void likeIsIdempotent() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("글", "내용"));

        assertThat(postService.like(postId, OTHER)).isTrue();
        assertThat(postService.like(postId, OTHER)).isFalse();
        assertThat(postLikeRepository.countByPostId(postId)).isEqualTo(1);
    }

    @Test
    @DisplayName("★멱등★ 추천하지 않은 글을 취소해도 예외가 아니라 false")
    void unlikeIsIdempotent() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("글", "내용"));

        assertThat(postService.unlike(postId, OTHER)).isFalse();
    }

    @Test
    @DisplayName("추천하면 likedByMe가 true가 된다")
    void likedByMe() {
        Long postId = postService.createPost(workId, AUTHOR, new PostRequest("글", "내용"));
        postService.like(postId, OTHER);

        assertThat(postService.getPost(postId, OTHER).likedByMe()).isTrue();
        assertThat(postService.getPost(postId, AUTHOR).likedByMe()).isFalse();
    }
}
