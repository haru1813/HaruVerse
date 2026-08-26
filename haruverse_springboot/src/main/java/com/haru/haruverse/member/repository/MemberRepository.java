package com.haru.haruverse.member.repository;

import com.haru.haruverse.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 회원 저장소 — JpaRepository가 기본 CRUD 제공. 메서드명으로 쿼리 자동 생성.
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}
