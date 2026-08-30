package com.haru.haruverse.admin.dto;

import com.haru.haruverse.member.entity.Member;

import java.time.LocalDateTime;

/**
 * 관리자 회원 목록의 한 줄.
 *
 * <p><b>★엔티티를 그대로 내보내지 않는 이유★</b>
 * {@link Member} 에는 비밀번호 해시가 들어 있다. 엔티티를 반환하면 잭슨이
 * 그대로 직렬화해 <b>해시가 통째로 응답에 실린다.</b>
 * 관리자만 보는 화면이라도 내보낼 이유가 전혀 없고,
 * 한 번 새어 나간 해시는 오프라인에서 마음껏 공격당한다.
 *
 * <p>그래서 필요한 필드만 골라 담는 DTO 를 둔다. 필드를 추가할 때
 * "이걸 내보내도 되는가"를 한 번 더 생각하게 만드는 장치이기도 하다.
 */
public record AdminMemberResponse(
        Long id,
        String email,
        String nickname,
        String role,
        LocalDateTime createdAt
) {
    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name(),
                member.getCreatedAt());
    }
}
