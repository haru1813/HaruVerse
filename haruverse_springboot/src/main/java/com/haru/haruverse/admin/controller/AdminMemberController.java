package com.haru.haruverse.admin.controller;

import com.haru.haruverse.admin.dto.AdminMemberResponse;
import com.haru.haruverse.admin.service.AdminMemberService;
import com.haru.haruverse.member.entity.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 회원 관리 API.
 *
 * <p>{@code /api/admin/**} 는 SecurityConfig 에서 ADMIN 으로 잠겨 있다.
 */
@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final AdminMemberService memberService;

    public AdminMemberController(AdminMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminMemberResponse>> list(
            @RequestParam(required = false) String keyword,
            // 최근 가입자가 위로 — 관리 화면에서 먼저 보고 싶은 건 새로 들어온 사람이다
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(memberService.list(keyword, pageable));
    }

    /**
     * 권한 변경.
     *
     * <p>★현재 로그인한 관리자가 누구인지 서비스에 넘긴다★
     * '자기 자신은 못 바꾼다'를 판단하려면 그 정보가 필요하다.
     * 클라이언트가 보낸 값이 아니라 <b>토큰에서 나온 subject</b> 를 쓴다 —
     * 요청 본문으로 받으면 남의 이메일을 적어 자물쇠를 우회할 수 있다.
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminMemberResponse> changeRole(
            @PathVariable Long id,
            @RequestBody RoleRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                memberService.changeRole(id, request.role(), authentication.getName()));
    }

    /** 권한 변경 요청 본문 — {@code {"role": "ADMIN"}} */
    public record RoleRequest(MemberRole role) {}
}
