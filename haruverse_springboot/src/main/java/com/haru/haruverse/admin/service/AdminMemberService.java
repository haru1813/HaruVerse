package com.haru.haruverse.admin.service;

import com.haru.haruverse.admin.dto.AdminMemberResponse;
import com.haru.haruverse.admin.entity.AuditAction;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.entity.MemberRole;
import com.haru.haruverse.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 관리자 회원 관리.
 *
 * <p><b>★이 클래스의 존재 이유는 목록 조회가 아니라 '자물쇠'다★</b>
 * 권한 변경은 한 줄짜리 UPDATE 지만, 그 한 줄이 <b>관리자 콘솔 전체를 잠가버릴 수</b> 있다.
 * 관리자가 자기 계정을 USER 로 내리면 그 순간 콘솔에 들어갈 수 있는 사람이 사라지고,
 * 복구 수단은 DB 를 직접 고치는 것뿐이다.
 *
 * <p>그래서 두 가지를 <b>서비스 계층에서</b> 막는다. 화면에서 버튼을 감추는 걸로는 부족하다 —
 * API 는 curl 로도 부를 수 있다.
 * <ol>
 *   <li><b>자기 자신의 권한은 바꿀 수 없다</b></li>
 *   <li><b>마지막 관리자는 강등할 수 없다</b></li>
 * </ol>
 */
@Service
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final AuditService auditService;

    public AdminMemberService(MemberRepository memberRepository, AuditService auditService) {
        this.memberRepository = memberRepository;
        this.auditService = auditService;
    }

    /**
     * 회원 목록 — 키워드가 있으면 이메일·닉네임에서 찾는다.
     *
     * @param keyword 비어 있으면 전체 목록
     */
    @Transactional(readOnly = true)
    public Page<AdminMemberResponse> list(String keyword, Pageable pageable) {
        Page<Member> members = (keyword == null || keyword.isBlank())
                ? memberRepository.findAll(pageable)
                : memberRepository.findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), pageable);

        return members.map(AdminMemberResponse::from);
    }

    /**
     * 권한 변경.
     *
     * @param targetId     바꿀 회원
     * @param newRole      새 권한
     * @param currentEmail 지금 요청한 관리자 (토큰의 subject)
     *
     * @throws IllegalStateException 자기 자신이거나, 마지막 관리자를 내리려 할 때
     */
    @Transactional
    public AdminMemberResponse changeRole(Long targetId, MemberRole newRole, String currentEmail) {
        Member target = memberRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        // ── 자물쇠 ① 자기 자신 ──
        //   실수로 스스로를 내리는 걸 막는다. 권한을 올리는 것도 막는데,
        //   자기 권한을 스스로 조작할 수 있으면 그것 자체가 감사 추적을 흐린다.
        if (target.getEmail().equals(currentEmail)) {
            throw new IllegalStateException("자기 자신의 권한은 바꿀 수 없습니다.");
        }

        // ── 자물쇠 ② 마지막 관리자 ──
        //   ADMIN → USER 로 내리는 경우에만 본다. 올리는 건 언제나 안전하다.
        if (target.getRole() == MemberRole.ADMIN && newRole != MemberRole.ADMIN
                && memberRepository.countByRole(MemberRole.ADMIN) <= 1) {
            throw new IllegalStateException(
                    "마지막 관리자는 강등할 수 없습니다. 다른 관리자를 먼저 지정하세요.");
        }

        MemberRole before = target.getRole();
        target.changeRole(newRole);
        // 더티 체킹으로 커밋 시 UPDATE 가 나간다 (save 호출 불필요)

        auditService.record(currentEmail, AuditAction.CHANGE_ROLE, targetId,
                "%s(%s) %s → %s".formatted(
                        target.getNickname(), target.getEmail(), before, newRole));

        return AdminMemberResponse.from(target);
    }
}
