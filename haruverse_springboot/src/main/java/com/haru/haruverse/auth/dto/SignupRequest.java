package com.haru.haruverse.auth.dto;

// 회원가입 요청 바디 (프론트 features/auth/Signup.tsx 와 대응).
// TODO(하루): 검증이 필요하면 spring-boot-starter-validation 추가 후 @NotBlank 등 부착
public record SignupRequest(
        String email,
        String password,
        String nickname
) {}
