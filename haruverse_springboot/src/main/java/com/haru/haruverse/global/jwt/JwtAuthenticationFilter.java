package com.haru.haruverse.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// 매 요청마다 한 번 실행 — Authorization 헤더의 Bearer 토큰을 검사해
// 유효하면 SecurityContext에 '인증됨' 상태를 심는다. (스테이트리스)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && tokenProvider.validate(token)) {
            String email = tokenProvider.getEmail(token);
            // principal = 이메일, 권한 = 토큰의 role 클레임.
            // SecurityConfig의 hasRole("ADMIN")이 여기 담긴 "ROLE_ADMIN"을 본다.
            var authority = new SimpleGrantedAuthority(tokenProvider.getRole(token).authority());
            var authentication = new UsernamePasswordAuthenticationToken(email, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response); // 토큰이 없어도 통과 → 인가는 SecurityConfig가 판단
    }

    // "Authorization: Bearer xxx" 에서 xxx 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
