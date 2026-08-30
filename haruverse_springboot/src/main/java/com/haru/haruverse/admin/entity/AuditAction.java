package com.haru.haruverse.admin.entity;

/**
 * 기록할 관리자 행위.
 *
 * <p><b>★모든 행위를 기록하지는 않는다★</b>
 * 조회는 남기지 않는다 — 관리자가 목록을 여는 것까지 적으면 표가 순식간에
 * 수천 줄이 되고, 정작 중요한 '되돌릴 수 없는 변경'이 그 안에 묻힌다.
 * <b>데이터를 바꾸거나 없애는 것</b>만 남긴다.
 */
public enum AuditAction {
    /** 게시글 삭제 — 댓글·추천까지 함께 사라진다 */
    DELETE_POST("게시글 삭제"),
    /** 댓글(또는 답글) 삭제 */
    DELETE_COMMENT("댓글 삭제"),
    /** 회원 권한 변경 */
    CHANGE_ROLE("권한 변경");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    /** 화면에 그대로 쓰는 한국어 이름 */
    public String label() {
        return label;
    }
}
