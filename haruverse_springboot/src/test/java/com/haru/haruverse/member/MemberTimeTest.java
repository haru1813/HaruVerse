package com.haru.haruverse.member;

import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.entity.MemberRole;
import com.haru.haruverse.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회원의 생성·수정 시각.
 *
 * <p>{@code Member} 가 {@link com.haru.haruverse.global.common.BaseTimeEntity} 를
 * 상속하도록 바꾸면서 고정하는 것들이다.
 *
 * <p><b>★왜 테스트가 필요했나★</b>
 * 처음 상속을 붙였을 때 {@code Member} 안에 {@code createdAt} 필드와
 * {@code @PrePersist} 가 그대로 남아 있었다. 부모 필드를 가려버려(shadowing)
 * <b>updated_at 이 채워지지 않았는데 오류는 나지 않았다.</b>
 * 조용히 잘못되는 종류라 테스트로 잡아두어야 한다.
 */
@SpringBootTest
@Transactional
class MemberTimeTest {

    @Autowired MemberRepository memberRepository;

    @Test
    @DisplayName("저장하면 생성·수정 시각이 함께 채워진다")
    void bothFilledOnCreate() {
        Member member = memberRepository.save(
                new Member("time-a@haru.test", "encoded", "시각테스트"));
        memberRepository.flush();

        assertThat(member.getCreatedAt()).isNotNull();
        assertThat(member.getUpdatedAt()).isNotNull();
        // 처음에는 둘이 같다
        assertThat(member.getUpdatedAt()).isEqualTo(member.getCreatedAt());
    }

    @Test
    @DisplayName("★수정하면 updatedAt 만 바뀐다★ (createdAt 은 그대로)")
    void onlyUpdatedAtChangesOnModify() throws InterruptedException {
        Member member = memberRepository.save(
                new Member("time-b@haru.test", "encoded", "수정테스트"));
        memberRepository.flush();

        LocalDateTime createdAt = member.getCreatedAt();
        LocalDateTime before = member.getUpdatedAt();

        Thread.sleep(10); // 시각이 확실히 벌어지도록

        member.changeRole(MemberRole.ADMIN);
        memberRepository.flush(); // UPDATE 를 지금 내보낸다 → @PreUpdate 발동

        // ★updatable = false 라 생성 시각은 UPDATE 문에 포함되지 않는다★
        assertThat(member.getCreatedAt()).isEqualTo(createdAt);
        assertThat(member.getUpdatedAt()).isAfter(before);
    }

    @Test
    @DisplayName("비밀번호를 바꿔도 수정 시각이 남는다")
    void passwordChangeTouchesUpdatedAt() throws InterruptedException {
        Member member = memberRepository.save(
                new Member("time-c@haru.test", "encoded", "비번테스트"));
        memberRepository.flush();

        LocalDateTime before = member.getUpdatedAt();
        Thread.sleep(10);

        member.changePassword("new-encoded-value");
        memberRepository.flush();

        assertThat(member.getUpdatedAt()).isAfter(before);
    }
}
