package com.haru.haruverse.community.controller;

import com.haru.haruverse.community.dto.*;
import com.haru.haruverse.community.service.PostService;
import com.haru.haruverse.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 커뮤니티 API.
 *
 * <p><b>게시판 = 작품</b>이라 글 목록·작성은 작품 아래 경로에 둔다.
 * 글이 만들어진 뒤에는 어느 작품인지가 글에 담겨 있으므로 {@code /api/posts/{id}} 로 짧게 간다.
 *
 * <p>인가는 SecurityConfig가 처리한다 — GET만 공개, 나머지는 인증 필수.
 */
@RestController
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /* ── 글 ───────────────────────────────────────────── */

    /**
     * 채널 목록 — GET /api/community/channels
     *
     * <p>커뮤니티 첫 화면. 글이 있는 작품만 카드로, 각 카드에 최근 글이 담긴다.
     */
    @GetMapping("/api/community/channels")
    public ResponseEntity<PageResponse<ChannelResponse>> getChannels(
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(postService.getChannels(pageable));
    }

    /**
     * 전체 최근 글 — GET /api/posts
     *
     * <p>커뮤니티 첫 화면. 게시판(작품)이 187개라 목록만 두면 빈 게시판이 대부분이라,
     * 어느 게시판이든 새 글을 한곳에 모아 보여준다.
     */
    @GetMapping("/api/posts")
    public ResponseEntity<PageResponse<RecentPostResponse>> getRecentPosts(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.getRecentPosts(pageable));
    }

    /** 작품 게시판 목록 — GET /api/works/{workId}/posts */
    @GetMapping("/api/works/{workId}/posts")
    public ResponseEntity<PageResponse<PostSummaryResponse>> getPosts(
            @PathVariable Long workId,
            // 정렬은 쿼리에 최신순으로 고정돼 있어 sort를 받지 않는다
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.getPosts(workId, pageable));
    }

    /**
     * 글 상세 — GET /api/posts/{id}
     *
     * <p>비로그인도 볼 수 있다. 토큰이 있으면 principal이 들어와
     * '내가 추천했는지·내 글인지'가 함께 계산된다.
     */
    @GetMapping("/api/posts/{id}")
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(postService.getPost(id, email));
    }

    /** 글 작성 — POST /api/works/{workId}/posts */
    @PostMapping("/api/works/{workId}/posts")
    public ResponseEntity<Void> createPost(
            @PathVariable Long workId,
            @AuthenticationPrincipal String email,
            @RequestBody PostRequest request) {
        Long id = postService.createPost(workId, email, request);
        // 201 + Location — 만들어진 자원의 위치를 알려주는 게 REST 관례
        return ResponseEntity.created(URI.create("/api/posts/" + id)).build();
    }

    /** 글 수정 — 작성자만 (아니면 403) */
    @PutMapping("/api/posts/{id}")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long id,
            @AuthenticationPrincipal String email,
            @RequestBody PostRequest request) {
        postService.updatePost(id, email, request);
        return ResponseEntity.noContent().build();
    }

    /** 글 삭제 — 작성자만 (댓글·추천도 함께 지워진다) */
    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        postService.deletePost(id, email);
        return ResponseEntity.noContent().build();
    }

    /* ── 댓글 ─────────────────────────────────────────── */

    @GetMapping("/api/posts/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(postService.getComments(id, email));
    }

    @PostMapping("/api/posts/{id}/comments")
    public ResponseEntity<Void> createComment(
            @PathVariable Long id,
            @AuthenticationPrincipal String email,
            @RequestBody CommentRequest request) {
        postService.createComment(id, email, request);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal String email) {
        postService.deleteComment(commentId, email);
        return ResponseEntity.noContent().build();
    }

    /* ── 추천 ─────────────────────────────────────────── */

    /**
     * 추천 — 찜과 같은 방식으로 PUT/DELETE를 쓴다.
     * 토글(POST)은 재시도·더블클릭에서 상태가 뒤집혀 멱등하지 않다.
     */
    @PutMapping("/api/posts/{id}/like")
    public ResponseEntity<Void> like(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        boolean created = postService.like(id, email);
        return created ? ResponseEntity.status(201).build() : ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/posts/{id}/like")
    public ResponseEntity<Void> unlike(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        postService.unlike(id, email);
        return ResponseEntity.noContent().build();
    }
}
