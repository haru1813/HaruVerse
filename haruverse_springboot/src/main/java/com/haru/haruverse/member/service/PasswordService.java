package com.haru.haruverse.member.service;

import com.haru.haruverse.member.dto.PasswordChangeRequest;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 비밀번호 변경.
 *
 * <p><b>★본인 것만 바꾼다★</b>
 * 관리자가 남의 비밀번호를 재설정하는 기능은 <b>일부러 만들지 않았다.</b>
 * 그게 있으면 관리자가 아무 계정에나 들어갈 수 있게 되고,
 * "누가 그 계정으로 한 일인가"를 나중에 가릴 수 없다.
 * 비밀번호를 잊은 회원은 재설정 메일 같은 별도 경로로 풀어야 할 문제다.
 */
@Service
public class PasswordService {

    /**
     * 최소 길이.
     *
     * <p>특수문자·대소문자 조합을 강제하지 않는다 — NIST 권고도 그쪽이다.
     * 규칙이 복잡할수록 사람은 {@code Password1!} 같은 예측 가능한 형태로 수렴하고,
     * 실제 강도는 <b>길이</b>가 좌우한다.
     */
    private static final int MIN_LENGTH = 8;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 로그인한 본인의 비밀번호를 바꾼다.
     *
     * @param email 토큰에서 나온 subject — 요청 본문이 아니다
     *
     * @throws IllegalStateException 현재 비밀번호 불일치·형식 위반
     */
    @Transactional
    public void change(String email, PasswordChangeRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        String current = request == null ? null : request.currentPassword();
        String next = request == null ? null : request.newPassword();

        // ★IllegalArgumentException 을 쓰지 않는다★
        //   GlobalExceptionHandler 가 그걸 401 로 매핑한다. 그러면 프론트가
        //   '토큰 만료'로 보고 로그아웃시켜, 비밀번호를 잘못 친 사람이 튕겨 나간다.
        //   409 가 되도록 IllegalStateException 을 쓴다.
        if (current == null || current.isBlank()) {
            throw new IllegalStateException("현재 비밀번호를 입력해 주세요.");
        }
        if (!passwordEncoder.matches(current, member.getPassword())) {
            throw new IllegalStateException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (next == null || next.length() < MIN_LENGTH) {
            throw new IllegalStateException("새 비밀번호는 %d자 이상이어야 합니다.".formatted(MIN_LENGTH));
        }
        if (current.equals(next)) {
            throw new IllegalStateException("현재 비밀번호와 다른 값을 입력해 주세요.");
        }

        member.changePassword(passwordEncoder.encode(next));
        // 더티 체킹으로 커밋 시 UPDATE 가 나간다
    }
}
