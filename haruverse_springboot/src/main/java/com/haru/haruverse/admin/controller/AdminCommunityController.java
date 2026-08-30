package com.haru.haruverse.admin.controller;

import com.haru.haruverse.admin.dto.AdminCommentResponse;
import com.haru.haruverse.admin.dto.AdminPostResponse;
import com.haru.haruverse.admin.service.AdminCommunityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 커뮤니티 관리 API.
 *
 * <p>{@code /api/admin/**} 는 SecurityConfig 에서 ADMIN 으로 잠겨 있다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminCommunityController {

    private final AdminCommunityService communityService;

    public AdminCommunityController(AdminCommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/posts")
    public ResponseEntity<Page<AdminPostResponse>> posts(
            @RequestParam(required = false) String keyword,
            // 정렬은 쿼리가 이미 createdAt desc 로 고정한다 (group by 가 있어 Pageable 정렬과 섞이면 깨진다)
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(communityService.listPosts(keyword, pageable));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        communityService.deletePost(id);
        // 204 — 본문 없음. 프론트의 api.ts 가 이 경우 json() 을 부르지 않는다
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/comments")
    public ResponseEntity<Page<AdminCommentResponse>> comments(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(communityService.listComments(keyword, pageable));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        communityService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
