package com.haru.haruverse.global.config;

import com.haru.haruverse.global.jwt.JwtAuthenticationFilter;
import com.haru.haruverse.global.jwt.JwtTokenProvider;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 앱 전역 보안 설정 — JWT 기반 스테이트리스 인증.
@Configuration
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    public SecurityConfig(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // JWT는 세션 대신 토큰 → CSRF 불필요
            .csrf(csrf -> csrf.disable())
            // H2 콘솔 iframe 허용
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // 세션을 만들지 않음 (매 요청 토큰으로 인증)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 인가 규칙
            .authorizeHttpRequests(auth -> auth
                    // ★ERROR 디스패치를 열어둔다★
                    //   403을 낼 때 스프링은 response.sendError(403)을 부르고, 그러면
                    //   서블릿이 /error 로 <b>다시 디스패치</b>한다. 그 재요청도 이 필터 체인을 타는데,
                    //   그때는 SecurityContext가 비어 있어 anyRequest().authenticated()에 걸린다.
                    //   → 원래 내려던 403이 401로 덮인다("권한 없음"이 "로그인하라"로 바뀐다).
                    //   MockMvc는 ERROR 디스패치를 타지 않아 테스트에서는 403이 보여서, 이 차이가 잘 안 드러난다.
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                    // 인증 없이 열어둘 곳: 로그인·회원가입, 헬스체크, H2 콘솔
                    .requestMatchers("/api/auth/**", "/api/health", "/h2-console/**").permitAll()
                    // 작품 조회는 비로그인도 볼 수 있어야 함 (설계문서 ④ work 섹션 — 인증 '-')
                    // GET만 열고, 나중에 생길 등록·수정(POST/PUT)은 인증이 필요하도록 메서드를 한정
                    .requestMatchers(HttpMethod.GET, "/api/works/**").permitAll()
                    // 캐릭터 도감도 같은 취급 — 로그인 없이 볼 수 있어야 한다
                    // (작품별 캐릭터 /api/works/{id}/characters 는 위 규칙에 이미 포함된다)
                    .requestMatchers(HttpMethod.GET, "/api/characters/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/studios/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/voice-actors/**").permitAll()
                    // 커뮤니티 — 글·댓글 '읽기'는 비로그인도 가능.
                    // 쓰기(POST/PUT/DELETE)는 아래 anyRequest().authenticated()에 걸린다.
                    .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/community/**").permitAll()
                    // ★수집 API는 관리자만★
                    //   /api/collect/** 는 Jikan·RAWG를 대신 호출한다. 로그인만 하면
                    //   누구나 부를 수 있게 두면, 가입한 아무나 외부 API 쿼터를 소진시키고
                    //   DB를 오염시킬 수 있다. 공개 배포 전에 반드시 닫아야 하는 구멍이었다.
                    //   승격은 DB에서 직접 한다 (관리자 화면이 아직 없다).
                    .requestMatchers("/api/collect/**").hasRole("ADMIN")
                    // 색인 관리도 같은 이유로 관리자만. 재색인은 DB 전체를 읽어 ES에 미는
                    // 작업이라 반복 호출 자체가 부하다.
                    // ★POST 만 잠근다★ — 검색(GET /api/search/...)은 공개여야 한다.
                    .requestMatchers(HttpMethod.POST, "/api/search/**").hasRole("ADMIN")
                    // ★관리자 콘솔 전용 API★
                    //   통계·회원 관리 등 운영자만 봐야 하는 것들이 여기 모인다.
                    //   경로 하나로 묶어 두면 API를 추가할 때마다 권한 설정을 다시
                    //   손대지 않아도 된다 — '잠그는 걸 잊는' 사고를 구조로 막는다.
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // 그 외는 토큰 필요
                    .anyRequest().authenticated())
            // 인증 안 된 요청 → 401 (기본 403 대신 명확하게)
            .exceptionHandling(e -> e.authenticationEntryPoint(
                    (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            // 우리 JWT 필터를 표준 인증 필터 앞에 끼움
            .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 비밀번호 해시 인코더 — 전역 빈
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
