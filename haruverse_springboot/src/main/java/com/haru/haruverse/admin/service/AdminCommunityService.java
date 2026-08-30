package com.haru.haruverse.admin.service;

import com.haru.haruverse.admin.dto.AdminCommentResponse;
import com.haru.haruverse.admin.dto.AdminPostResponse;
import com.haru.haruverse.admin.entity.AuditAction;
import com.haru.haruverse.community.entity.Comment;
import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 관리자 커뮤니티 관리.
 *
 * <p><b>★삭제를 직접 구현하지 않는다★</b>
 * 글을 지우려면 댓글·추천을 먼저 지워야 하는데(FK), 그 순서 지식은
 * {@link PostService} 안에 이미 있다. 여기서 다시 쓰면 순서가 두 곳으로 갈라지고
 * 나중에 연관 테이블이 늘었을 때 한쪽만 고쳐 FK 위반이 난다.
 * 그래서 조회와 <b>기록</b>만 여기서 하고, 삭제는 {@code PostService} 에 위임한다.
 */
@Service
public class AdminCommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostService postService;
    private final AuditService auditService;

    public AdminCommunityService(PostRepository postRepository,
                                 CommentRepository commentRepository,
                                 PostService postService,
                                 AuditService auditService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AdminPostResponse> listPosts(String keyword, Pageable pageable) {
        return postRepository.findForAdmin(normalize(keyword), pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminCommentResponse> listComments(String keyword, Pageable pageable) {
        return commentRepository.findForAdmin(normalize(keyword), pageable);
    }

    /**
     * 글 삭제 — 댓글·추천까지 함께 사라진다. <b>되돌릴 수 없다.</b>
     *
     * <p><b>★요약을 지우기 전에 만든다★</b>
     * 감사 로그에 제목·작성자를 남기려면 원본을 읽어야 하는데,
     * 삭제한 뒤에는 읽을 곳이 없다. 순서가 뒤바뀌면 기록이 비어버린다.
     *
     * <p>기록은 삭제와 <b>같은 트랜잭션</b>에서 남는다. 삭제가 실패해 롤백되면
     * 기록도 함께 사라진다 — 일어나지 않은 일이 기록에 남는 것보다 낫다.
     */
    @Transactional
    public void deletePost(Long postId, String actorEmail) {
        Post post = postRepository.findByIdWithMemberAndWork(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. id=" + postId));

        long commentCount = commentRepository.countByPostId(postId);
        String summary = "제목: %s / 작성자: %s / 게시판: %s / 함께 삭제된 댓글: %d개".formatted(
                post.getTitle(), post.getMember().getNickname(), post.getWork().getTitle(), commentCount);

        postService.deletePostAsAdmin(postId);
        auditService.record(actorEmail, AuditAction.DELETE_POST, postId, summary);
    }

    /** 댓글 삭제 — 최상위 댓글이면 딸린 답글도 함께 사라진다 */
    @Transactional
    public void deleteComment(Long commentId, String actorEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다. id=" + commentId));

        long replyCount = comment.isReply() ? 0 : commentRepository.countByParentId(commentId);
        String summary = "%s / 작성자: %s / 원글: %s / 내용: %s%s".formatted(
                comment.isReply() ? "답글" : "댓글",
                comment.getMember().getNickname(),
                comment.getPost().getTitle(),
                comment.getContent(),
                replyCount > 0 ? " / 함께 삭제된 답글: %d개".formatted(replyCount) : "");

        postService.deleteCommentAsAdmin(commentId);
        auditService.record(actorEmail, AuditAction.DELETE_COMMENT, commentId, summary);
    }

    /** 빈 문자열과 null 을 하나로 맞춘다 — 쿼리에서 둘 다 검사하지 않도록 */
    private String normalize(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }
}
