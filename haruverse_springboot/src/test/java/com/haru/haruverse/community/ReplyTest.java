package com.haru.haruverse.community;

import com.haru.haruverse.community.dto.CommentRequest;
import com.haru.haruverse.community.dto.CommentResponse;
import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.service.PostService;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 대댓글(답글).
 *
 * <p>고정하는 규칙은 넷이다.
 * <ol>
 *   <li><b>깊이는 1단계</b> — 답글에 답글을 달 수 없다</li>
 *   <li><b>답글은 부모 아래 중첩</b>되어 나간다</li>
 *   <li><b>부모를 지우면 답글도 사라진다</b> — 자기참조 FK 라 순서를 어기면 터진다</li>
 *   <li><b>남의 글 댓글을 부모로 지정할 수 없다</b></li>
 * </ol>
 */
@SpringBootTest
@Transactional
class ReplyTest {

    @Autowired PostService postService;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    private static final String AUTHOR = "reply-author@haru.test";
    private static final String OTHER = "reply-other@haru.test";

    private Post post;
    private Long rootId;

    @BeforeEach
    void setUp() {
        Member author = memberRepository.save(new Member(AUTHOR, "encoded", "글쓴이"));
        memberRepository.save(new Member(OTHER, "encoded", "다른사람"));

        Work work = workRepository.save(new Work("답글테스트", WorkType.ANIME, WorkSource.JIKAN));
        post = postRepository.save(new Post(work, author, "제목", "본문"));

        rootId = postService.createComment(post.getId(), AUTHOR, new CommentRequest("최상위 댓글", null));
    }

    @Test
    @DisplayName("답글이 부모 아래 중첩되어 나온다")
    void repliesAreNested() {
        postService.createComment(post.getId(), OTHER, new CommentRequest("첫 번째 답글", rootId));
        postService.createComment(post.getId(), AUTHOR, new CommentRequest("두 번째 답글", rootId));

        List<CommentResponse> comments = postService.getComments(post.getId(), AUTHOR);

        // 최상위는 하나뿐 — 답글이 목록에 따로 끼어들면 안 된다
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).content()).isEqualTo("최상위 댓글");

        List<CommentResponse> replies = comments.get(0).replies();
        assertThat(replies).hasSize(2);
        // 오래된 순
        assertThat(replies.get(0).content()).isEqualTo("첫 번째 답글");
        assertThat(replies.get(1).content()).isEqualTo("두 번째 답글");
        // 답글에는 답글이 없다
        assertThat(replies.get(0).replies()).isEmpty();
    }

    @Test
    @DisplayName("★답글에는 다시 답글을 달 수 없다★ (깊이 1단계)")
    void cannotReplyToReply() {
        Long replyId = postService.createComment(
                post.getId(), OTHER, new CommentRequest("답글", rootId));

        assertThatThrownBy(() -> postService.createComment(
                post.getId(), AUTHOR, new CommentRequest("답글의 답글", replyId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("답글에는");
    }

    @Test
    @DisplayName("★다른 글의 댓글을 부모로 지정할 수 없다★")
    void parentMustBelongToSamePost() {
        Member author = memberRepository.findByEmail(AUTHOR).orElseThrow();
        Work work = workRepository.save(new Work("다른작품", WorkType.ANIME, WorkSource.JIKAN));
        Post another = postRepository.save(new Post(work, author, "다른 글", "본문"));

        assertThatThrownBy(() -> postService.createComment(
                another.getId(), AUTHOR, new CommentRequest("끼어들기", rootId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이 글의 댓글이 아닙니다");
    }

    @Test
    @DisplayName("★부모 댓글을 지우면 답글도 사라진다★ (자기참조 FK)")
    void deletingParentRemovesReplies() {
        postService.createComment(post.getId(), OTHER, new CommentRequest("답글1", rootId));
        postService.createComment(post.getId(), OTHER, new CommentRequest("답글2", rootId));
        assertThat(commentRepository.countByParentId(rootId)).isEqualTo(2);

        postService.deleteComment(rootId, AUTHOR);

        assertThat(commentRepository.findById(rootId)).isEmpty();
        assertThat(commentRepository.countByPostId(post.getId())).isZero();
    }

    @Test
    @DisplayName("★글을 지우면 답글까지 전부 사라진다★ — 순서를 어기면 FK 위반이 난다")
    void deletingPostRemovesRepliesToo() {
        postService.createComment(post.getId(), OTHER, new CommentRequest("답글", rootId));

        postService.deletePost(post.getId(), AUTHOR);

        assertThat(postRepository.findById(post.getId())).isEmpty();
        assertThat(commentRepository.countByPostId(post.getId())).isZero();
    }

    @Test
    @DisplayName("답글만 따로 지우면 부모는 남는다")
    void deletingReplyKeepsParent() {
        Long replyId = postService.createComment(
                post.getId(), OTHER, new CommentRequest("답글", rootId));

        postService.deleteComment(replyId, OTHER);

        assertThat(commentRepository.findById(replyId)).isEmpty();
        assertThat(commentRepository.findById(rootId)).isPresent();
    }
}
