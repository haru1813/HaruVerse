package com.haru.haruverse.member.dto;

/**
 * 비밀번호 변경 요청.
 *
 * <p><b>★현재 비밀번호를 반드시 함께 받는다★</b>
 * 토큰만으로 바꾸게 두면, 남의 브라우저가 잠깐 열려 있는 사이나
 * 토큰이 새어 나갔을 때 <b>계정을 통째로 빼앗기는 경로</b>가 된다.
 * 현재 비밀번호를 아는 사람만 바꿀 수 있어야 그 위험이 끊긴다.
 */
public record PasswordChangeRequest(
        String currentPassword,
        String newPassword
) {}
