package com.haru.haruverse.auth.service;

import com.haru.haruverse.auth.dto.LoginRequest;
import com.haru.haruverse.auth.dto.LoginResponse;
import com.haru.haruverse.auth.dto.SignupRequest;
import com.haru.haruverse.global.jwt.JwtTokenProvider;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.service.MemberService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 인증 도메인 서비스 — 회원가입·로그인 흐름을 처리 (member 도메인을 조회·저장에 활용).
@Service
public class AuthService {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(MemberService memberService, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    // 회원가입 — 이메일 중복 확인 → 비밀번호 인코딩 → 저장 → 생성된 회원 id 반환
    @Transactional
    public Long signup(SignupRequest req) {
        if (memberService.existsByEmail(req.email())) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }
        String encodedPassword = passwordEncoder.encode(req.password());
        Member member = new Member(req.email(), encodedPassword, req.nickname());
        return memberService.save(member).getId();
    }

    // 로그인 — 이메일 조회 → 비밀번호 대조 → 성공 시 JWT 발급
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        Member member = memberService.findByEmail(req.email())
                // 이메일이 없어도 "이메일/비번 중 무엇이 틀렸는지" 노출하지 않음 (보안)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 입력한 평문 비번 vs 저장된 해시 비교
        if (!passwordEncoder.matches(req.password(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = tokenProvider.createToken(member.getEmail(), member.getRole());
        return new LoginResponse(token, member.getEmail(), member.getNickname());
    }
}
