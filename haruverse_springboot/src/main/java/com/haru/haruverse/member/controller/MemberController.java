package com.haru.haruverse.member.controller;

import com.haru.haruverse.member.dto.MemberResponse;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 회원 API — /api/members/** 는 SecurityConfig에서 '인증 필요'로 보호됨.
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 내 정보 — JWT 필터가 넣어준 인증 주체(email)로 현재 회원을 조회.
    // 토큰이 없거나 유효하지 않으면 SecurityConfig에서 401로 막힘.
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(Authentication authentication) {
        String email = authentication.getName(); // principal = 토큰의 subject(email)
        Member member = memberService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return ResponseEntity.ok(new MemberResponse(member.getId(), member.getEmail(), member.getNickname()));
    }
}
