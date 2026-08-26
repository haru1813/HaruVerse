package com.haru.haruverse.auth.controller;

import com.haru.haruverse.auth.dto.LoginRequest;
import com.haru.haruverse.auth.dto.LoginResponse;
import com.haru.haruverse.auth.dto.SignupRequest;
import com.haru.haruverse.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 인증 API — 프론트 features/auth 의 로그인·회원가입 화면이 호출.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 회원가입 → 생성된 회원 id 반환
    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@RequestBody SignupRequest request) {
        Long memberId = authService.signup(request);
        return ResponseEntity.ok(memberId);
    }

    // 로그인 → JWT 토큰 + 기본 회원 정보 반환
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
