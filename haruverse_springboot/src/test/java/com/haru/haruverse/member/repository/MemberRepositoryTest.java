package com.haru.haruverse.member.repository;

import com.haru.haruverse.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// MemberRepository '슬라이스' 테스트 — JPA 관련 빈만 로드하고, 임베디드 H2에
// 실제로 저장/조회해 쿼리 메서드(findByEmail·existsByEmail)가 맞게 동작하는지 검증.
@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("existsByEmail: 저장된 이메일은 true, 없는 이메일은 false")
    void existsByEmail() {
        // given
        memberRepository.save(new Member("exist@haru.com", "pw", "존재"));

        // when & then
        assertThat(memberRepository.existsByEmail("exist@haru.com")).isTrue();
        assertThat(memberRepository.existsByEmail("none@haru.com")).isFalse();
    }

    @Test
    @DisplayName("findByEmail: 저장된 회원을 이메일로 조회한다")
    void findByEmail() {
        // given
        memberRepository.save(new Member("find@haru.com", "pw", "찾기"));

        // when
        Optional<Member> found = memberRepository.findByEmail("find@haru.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("찾기");
        assertThat(found.get().getCreatedAt()).isNotNull(); // @PrePersist로 생성시각 채워짐
    }
}
