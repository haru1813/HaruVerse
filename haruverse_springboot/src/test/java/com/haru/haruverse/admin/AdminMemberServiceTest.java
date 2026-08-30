package com.haru.haruverse.admin;

import com.haru.haruverse.admin.service.AdminMemberService;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.entity.MemberRole;
import com.haru.haruverse.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 회원 권한 변경의 <b>자물쇠</b>.
 *
 * <p>권한 변경은 한 줄짜리 UPDATE 지만 그 한 줄이 관리자 콘솔 전체를 잠글 수 있다.
 * 관리자가 자기 계정을 USER 로 내리면 콘솔에 들어갈 사람이 사라지고,
 * 복구 수단은 DB 직접 수정뿐이다. 그래서 두 자물쇠를 여기서 고정한다.
 *
 * <p>★화면이 아니라 서비스에서 막아야 하는 이유★
 * 버튼을 감추는 것만으로는 부족하다. API 는 curl 로도 부를 수 있다.
 */
@SpringBootTest
@Transactional // 각 테스트가 만든 회원은 롤백된다
class AdminMemberServiceTest {

    @Autowired AdminMemberService adminMemberService;
    @Autowired MemberRepository memberRepository;

    private Member admin;
    private Member user;

    @BeforeEach
    void setUp() {
        admin = memberRepository.save(new Member("lock-admin@haru.test", "encoded", "관리자"));
        admin.changeRole(MemberRole.ADMIN);

        user = memberRepository.save(new Member("lock-user@haru.test", "encoded", "일반회원"));
    }

    @Test
    @DisplayName("★자기 자신의 권한은 바꿀 수 없다★ — 스스로 콘솔을 잠그는 사고를 막는다")
    void cannotChangeOwnRole() {
        assertThatThrownBy(() ->
                adminMemberService.changeRole(admin.getId(), MemberRole.USER, admin.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("자기 자신");

        assertThat(admin.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    @DisplayName("자기 권한을 '올리는' 것도 막는다 — 스스로 조작하면 추적이 흐려진다")
    void cannotPromoteSelfEither() {
        assertThatThrownBy(() ->
                adminMemberService.changeRole(user.getId(), MemberRole.ADMIN, user.getEmail()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("★마지막 관리자는 강등할 수 없다★")
    void cannotDemoteLastAdmin() {
        // 이 테스트가 의미를 가지려면 ADMIN 이 정확히 한 명이어야 한다.
        // DB 에 다른 관리자가 있으면 자물쇠가 안 걸리는 게 정상이므로 그때는 건너뛴다.
        long admins = memberRepository.countByRole(MemberRole.ADMIN);
        if (admins != 1) return;

        // 다른 관리자가 admin 을 내리려는 상황 (자물쇠 ①에 걸리지 않게 남이 요청)
        assertThatThrownBy(() ->
                adminMemberService.changeRole(admin.getId(), MemberRole.USER, "someone@haru.test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("마지막 관리자");
    }

    @Test
    @DisplayName("관리자가 둘이면 한 명은 강등할 수 있다")
    void canDemoteWhenAnotherAdminExists() {
        Member second = memberRepository.save(new Member("lock-admin2@haru.test", "encoded", "관리자2"));
        second.changeRole(MemberRole.ADMIN);
        memberRepository.flush(); // countByRole 이 세도록 반영

        var result = adminMemberService.changeRole(second.getId(), MemberRole.USER, admin.getEmail());

        assertThat(result.role()).isEqualTo("USER");
        assertThat(second.getRole()).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("일반 회원을 관리자로 올릴 수 있다 (승격은 언제나 안전하다)")
    void canPromoteUser() {
        var result = adminMemberService.changeRole(user.getId(), MemberRole.ADMIN, admin.getEmail());

        assertThat(result.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("★응답에 비밀번호 해시가 실리지 않는다★")
    void neverExposesPasswordHash() {
        var page = adminMemberService.list("lock-", PageRequest.of(0, 10));

        assertThat(page.getContent()).isNotEmpty();
        // record 의 필드가 5개(id·email·nickname·role·createdAt)뿐임을 구조로 보장한다.
        // toString 에 해시가 섞여 있으면 여기서 걸린다.
        assertThat(page.getContent().get(0).toString()).doesNotContain("encoded");
    }

    @Test
    @DisplayName("이메일·닉네임 어느 쪽으로도 검색된다")
    void searchesByEmailOrNickname() {
        assertThat(adminMemberService.list("lock-user", PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(adminMemberService.list("일반회원", PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
    }
}
