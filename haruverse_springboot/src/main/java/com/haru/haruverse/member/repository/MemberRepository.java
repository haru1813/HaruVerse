package com.haru.haruverse.member.repository;

import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.entity.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 회원 저장소 — JpaRepository가 기본 CRUD 제공. 메서드명으로 쿼리 자동 생성.
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 권한별 회원 수 — 관리자 통계용 */
    long countByRole(MemberRole role);

    /**
     * 이메일 또는 닉네임으로 검색 — 관리자 회원 목록용.
     *
     * <p>메서드 이름이 길지만 규칙 그대로다:
     * {@code email LIKE %kw% OR nickname LIKE %kw%} (대소문자 무시).
     * 같은 키워드를 두 번 넘겨야 한다 — 이름 기반 쿼리는 파라미터를 재사용하지 못한다.
     */
    Page<Member> findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
            String email, String nickname, Pageable pageable);
}
