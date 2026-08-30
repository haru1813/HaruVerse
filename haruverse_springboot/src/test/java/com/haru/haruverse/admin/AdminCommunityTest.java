package com.haru.haruverse.admin;

import com.haru.haruverse.admin.service.AdminCommunityService;
import com.haru.haruverse.community.entity.Comment;
import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 커뮤니티 관리.
 *
 * <p>보는 것은 두 가지다.
 * <ol>
 *   <li><b>목록 쿼리가 실제로 도는가</b> — JPQL 은 컴파일로 걸리지 않는다.
 *       DTO 생성자 시그니처가 하나만 어긋나도 런타임에 터진다</li>
 *   <li><b>글을 지우면 댓글도 함께 사라지는가</b> — 순서를 어기면 FK 위반이 난다</li>
 * </ol>
 */
@SpringBootTest
@Transactional
class AdminCommunityTest {

    @Autowired AdminCommunityService adminCommunityService;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    private Post post;

    @BeforeEach
    void setUp() {
        Member author = memberRepository.save(new Member("com-author@haru.test", "encoded", "글쓴이"));

        Work work = workRepository.save(
                new Work("관리자테스트작품", WorkType.ANIME, WorkSource.JIKAN));

        post = postRepository.save(new Post(work, author, "테스트 제목", "테스트 본문입니다"));
        commentRepository.save(new Comment(post, author, "테스트 댓글"));
    }

    @Test
    @DisplayName("★게시글 목록 쿼리가 실제로 돈다★ (JPQL·DTO 생성자 검증)")
    void listPostsRuns() {
        var page = adminCommunityService.listPosts(null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isPositive();

        var row = page.getContent().stream()
                .filter(p -> p.id().equals(post.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(row.title()).isEqualTo("테스트 제목");
        assertThat(row.authorNickname()).isEqualTo("글쓴이");
        assertThat(row.workTitle()).isEqualTo("관리자테스트작품");
        assertThat(row.commentCount()).isEqualTo(1);
        // 본문은 잘려 들어온다 (목록에 10,000자를 실을 수 없다)
        assertThat(row.excerpt()).startsWith("테스트 본문");
    }

    @Test
    @DisplayName("★댓글 목록에 원글 제목이 함께 온다★ (맥락 없이는 판단할 수 없다)")
    void listCommentsCarriesPostTitle() {
        var page = adminCommunityService.listComments(null, PageRequest.of(0, 20));

        var row = page.getContent().stream()
                .filter(c -> c.postId().equals(post.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(row.content()).isEqualTo("테스트 댓글");
        assertThat(row.postTitle()).isEqualTo("테스트 제목");
        assertThat(row.authorNickname()).isEqualTo("글쓴이");
    }

    @Test
    @DisplayName("제목·본문·닉네임 어느 쪽으로도 검색된다")
    void searchesAcrossFields() {
        var byTitle = adminCommunityService.listPosts("테스트 제목", PageRequest.of(0, 20));
        var byContent = adminCommunityService.listPosts("본문입니다", PageRequest.of(0, 20));
        var byAuthor = adminCommunityService.listPosts("글쓴이", PageRequest.of(0, 20));

        assertThat(byTitle.getTotalElements()).isPositive();
        assertThat(byContent.getTotalElements()).isPositive();
        assertThat(byAuthor.getTotalElements()).isPositive();
    }

    @Test
    @DisplayName("★글을 지우면 댓글도 함께 사라진다★ — 순서를 어기면 FK 위반이 난다")
    void deletingPostRemovesComments() {
        Long postId = post.getId();
        assertThat(commentRepository.countByPostId(postId)).isEqualTo(1);

        adminCommunityService.deletePost(postId);

        assertThat(postRepository.findById(postId)).isEmpty();
        assertThat(commentRepository.countByPostId(postId)).isZero();
    }

    @Test
    @DisplayName("댓글만 따로 지울 수 있다 (글은 남는다)")
    void deletesCommentOnly() {
        Comment comment = commentRepository.findByPostIdWithMember(post.getId()).get(0);

        adminCommunityService.deleteComment(comment.getId());

        assertThat(commentRepository.findById(comment.getId())).isEmpty();
        assertThat(postRepository.findById(post.getId())).isPresent();
    }
}
