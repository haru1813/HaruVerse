package com.haru.haruverse.member.dto;

// 회원 정보 응답 — 비밀번호 등 민감 정보는 제외하고 노출용 필드만.
public record MemberResponse(
        Long id,
        String email,
        String nickname
) {}
