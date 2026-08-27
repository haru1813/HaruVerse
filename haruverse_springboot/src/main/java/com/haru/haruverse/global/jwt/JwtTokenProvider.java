package com.haru.haruverse.global.jwt;

import io.jsonwebtoken.Claims;
import com.haru.haruverse.member.entity.MemberRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWT 생성·검증 담당. 시크릿 키로 서명(HS256)한다.
@Component
public class JwtTokenProvider {

    private final String secret;
    private final long expirationMs;
    private SecretKey key;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    void init() {
        // 문자열 시크릿 → HMAC 서명 키 (32바이트 이상 필요)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 클레임 키 — 권한. 표준 클레임이 아니라 직접 정한 이름이다. */
    private static final String ROLE_CLAIM = "role";

    // 토큰 발급 — subject에 이메일(누구인지), role 클레임에 권한(무엇을 할 수 있는지)
    public String createToken(String email, MemberRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 토큰에서 이메일(subject) 추출
    public String getEmail(String token) {
        return parse(token).getSubject();
    }

    /**
     * 토큰에서 권한 추출.
     *
     * <p>★role 클레임이 없으면 USER로 본다★ — 이 기능을 넣기 전에 발급된 토큰이
     * 아직 살아 있기 때문이다(유효기간 1시간). 없다고 예외를 던지면
     * 배포 직후 한 시간 동안 기존 사용자가 전부 튕긴다.
     * 권한을 <b>낮은 쪽</b>으로 기본값을 잡는 게 안전한 방향이다.
     */
    public MemberRole getRole(String token) {
        String role = parse(token).get(ROLE_CLAIM, String.class);
        if (role == null) return MemberRole.USER;
        try {
            return MemberRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return MemberRole.USER; // 모르는 값 → 최소 권한
        }
    }

    // 서명·만료 검증 — 유효하면 true, 아니면 false
    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false; // 서명 불일치·만료·위조 등
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
