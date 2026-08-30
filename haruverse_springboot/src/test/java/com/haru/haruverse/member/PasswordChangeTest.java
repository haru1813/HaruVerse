package com.haru.haruverse.member;

import com.haru.haruverse.member.dto.PasswordChangeRequest;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.member.service.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 비밀번호 변경.
 *
 * <p>관리자 콘솔이 인터넷에 열리기 전에 비밀번호를 바꿀 수단이 필요해 만든 기능이다.
 * 여기서 고정하는 것은 <b>현재 비밀번호를 모르면 못 바꾼다</b>는 규칙과,
 * 실패가 <b>401 이 아니라 409</b> 로 나가야 한다는 점이다.
 */
@SpringBootTest
@Transactional
class PasswordChangeTest {

    @Autowired PasswordService passwordService;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String EMAIL = "pw-test@haru.test";
    private static final String CURRENT = "current-password-1";

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(
                new Member(EMAIL, passwordEncoder.encode(CURRENT), "비번테스트"));
    }

    @Test
    @DisplayName("현재 비밀번호가 맞으면 바뀐다")
    void changes() {
        passwordService.change(EMAIL, new PasswordChangeRequest(CURRENT, "brand-new-password"));

        assertThat(passwordEncoder.matches("brand-new-password", member.getPassword())).isTrue();
        // 저장된 값은 언제나 해시다 — 평문이 들어가면 여기서 걸린다
        assertThat(member.getPassword()).isNotEqualTo("brand-new-password");
    }

    @Test
    @DisplayName("★현재 비밀번호를 모르면 못 바꾼다★ — 토큰만으로 계정을 빼앗기지 않게")
    void wrongCurrentRejected() {
        assertThatThrownBy(() ->
                passwordService.change(EMAIL, new PasswordChangeRequest("wrong", "brand-new-password")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("현재 비밀번호");

        assertThat(passwordEncoder.matches(CURRENT, member.getPassword())).isTrue();
    }

    @Test
    @DisplayName("8자 미만은 거부한다")
    void tooShortRejected() {
        assertThatThrownBy(() ->
                passwordService.change(EMAIL, new PasswordChangeRequest(CURRENT, "short7c")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("8자");
    }

    @Test
    @DisplayName("현재와 같은 값은 거부한다")
    void sameValueRejected() {
        assertThatThrownBy(() ->
                passwordService.change(EMAIL, new PasswordChangeRequest(CURRENT, CURRENT)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("다른 값");
    }

    @Test
    @DisplayName("★실패가 IllegalArgumentException 이면 안 된다★ (401 → 강제 로그아웃)")
    void failureMustNotMapTo401() {
        // GlobalExceptionHandler 는 IllegalArgumentException 을 401 로 매핑한다.
        // 비밀번호를 잘못 친 사람이 그 자리에서 로그아웃당하면 안 되므로
        // 이 서비스의 실패는 전부 IllegalStateException(→409) 이어야 한다.
        assertThatThrownBy(() ->
                passwordService.change(EMAIL, new PasswordChangeRequest("wrong", "brand-new-password")))
                .isNotInstanceOf(IllegalArgumentException.class);
    }
}
