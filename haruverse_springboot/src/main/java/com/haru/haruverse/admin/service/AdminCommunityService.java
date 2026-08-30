package com.haru.haruverse.admin.service;

import com.haru.haruverse.admin.dto.AdminCommentResponse;
import com.haru.haruverse.admin.dto.AdminPostResponse;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 커뮤니티 관리.
 *
 * <p><b>★삭제를 직접 구현하지 않는다★</b>
 * 글을 지우려면 댓글·추천을 먼저 지워야 하는데(FK), 그 순서 지식은
 * {@link PostService} 안에 이미 있다. 여기서 다시 쓰면 순서가 두 곳으로 갈라지고
 * 나중에 연관 테이블이 늘었을 때 한쪽만 고쳐 FK 위반이 난다.
 * 그래서 조회만 여기서 하고, 삭제는 {@code PostService} 에 위임한다.
 */
@Service
public class AdminCommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostService postService;

    public AdminCommunityService(PostRepository postRepository,
                                 CommentRepository commentRepository,
                                 PostService postService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postService = postService;
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
     * <p>이 프로젝트는 소프트 삭제를 쓰지 않는다. 그래서 화면에서 반드시
     * 무엇을 지우는지 보여주고 확인을 받아야 한다.
     */
    public void deletePost(Long postId) {
        postService.deletePostAsAdmin(postId);
    }

    public void deleteComment(Long commentId) {
        postService.deleteCommentAsAdmin(commentId);
    }

    /** 빈 문자열과 null 을 하나로 맞춘다 — 쿼리에서 둘 다 검사하지 않도록 */
    private String normalize(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }
}
