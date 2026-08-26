package com.haru.haruverse.auth.dto;

// 로그인 성공 응답 — 발급된 JWT + 기본 회원 정보.
public record LoginResponse(
        String token,
        String email,
        String nickname
) {}
