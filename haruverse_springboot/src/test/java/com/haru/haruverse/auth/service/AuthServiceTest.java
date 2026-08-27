package com.haru.haruverse.auth.service;

import com.haru.haruverse.auth.dto.LoginRequest;
import com.haru.haruverse.auth.dto.LoginResponse;
import com.haru.haruverse.auth.dto.SignupRequest;
import com.haru.haruverse.global.jwt.JwtTokenProvider;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.entity.MemberRole;
import com.haru.haruverse.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// AuthService '단위' 테스트 — 의존성을 Mockito 가짜로 대체해 signup·login 로직만 격리 검증.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    MemberService memberService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtTokenProvider tokenProvider;

    @InjectMocks
    AuthService authService; // 위 mock들이 주입됨

    // ---------- 회원가입 ----------

    @Test
    @DisplayName("회원가입 성공 시, 비밀번호는 인코딩되어 저장되고 회원 id를 반환한다")
    void signup_success() {
        SignupRequest req = new SignupRequest("new@haru.com", "raw-pw", "하루");
        given(memberService.existsByEmail("new@haru.com")).willReturn(false);
        given(passwordEncoder.encode("raw-pw")).willReturn("ENCODED");

        Member savedMember = new Member("new@haru.com", "ENCODED", "하루");
        ReflectionTestUtils.setField(savedMember, "id", 1L);
        given(memberService.save(any(Member.class))).willReturn(savedMember);

        Long id = authService.signup(req);

        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberService).save(captor.capture());
        Member toSave = captor.getValue();
        assertThat(toSave.getEmail()).isEqualTo("new@haru.com");
        assertThat(toSave.getPassword()).isEqualTo("ENCODED"); // 평문 아님
        assertThat(toSave.getNickname()).isEqualTo("하루");
    }

    @Test
    @DisplayName("이미 존재하는 이메일이면 예외를 던지고, 저장·인코딩은 수행하지 않는다")
    void signup_duplicate_email() {
        SignupRequest req = new SignupRequest("dup@haru.com", "pw", "닉네임");
        given(memberService.existsByEmail("dup@haru.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        verify(passwordEncoder, never()).encode(anyString());
        verify(memberService, never()).save(any());
    }

    // ---------- 로그인 ----------

    @Test
    @DisplayName("로그인 성공 시, 발급된 JWT와 회원 정보를 반환한다")
    void login_success() {
        Member member = new Member("a@haru.com", "HASH", "에이");
        given(memberService.findByEmail("a@haru.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("raw-pw", "HASH")).willReturn(true);
        given(tokenProvider.createToken("a@haru.com", MemberRole.USER)).willReturn("JWT-TOKEN");

        LoginResponse res = authService.login(new LoginRequest("a@haru.com", "raw-pw"));

        assertThat(res.token()).isEqualTo("JWT-TOKEN");
        assertThat(res.email()).isEqualTo("a@haru.com");
        assertThat(res.nickname()).isEqualTo("에이");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외를 던지고, 토큰을 발급하지 않는다")
    void login_wrong_password() {
        Member member = new Member("a@haru.com", "HASH", "에이");
        given(memberService.findByEmail("a@haru.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong", "HASH")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@haru.com", "wrong")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(tokenProvider, never()).createToken(anyString(), any());
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외를 던진다")
    void login_email_not_found() {
        given(memberService.findByEmail("none@haru.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@haru.com", "pw")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(tokenProvider, never()).createToken(anyString(), any());
    }
}
