package com.haru.haruverse.auth.dto;

// 로그인 요청 바디 (프론트 features/auth/Login.tsx 와 대응).
public record LoginRequest(
        String email,
        String password
) {}
