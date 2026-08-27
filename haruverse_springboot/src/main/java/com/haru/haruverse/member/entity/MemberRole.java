package com.haru.haruverse.member.entity;

/**
 * 회원 권한.
 *
 * <p>지금은 두 가지뿐이다. 권한 체계를 크게 만들 이유가 없어서
 * 문자열 role 하나로 두고, 필요해지면 그때 권한(Permission) 단위로 쪼갠다.
 *
 * <p>ADMIN이 필요한 이유는 <b>외부 API 수집</b> 하나다.
 * {@code /api/collect/**} 는 Jikan·RAWG를 대신 호출하는 엔드포인트라,
 * 아무나 부르면 외부 API 쿼터가 소진되고 DB가 오염된다.
 */
public enum MemberRole {
    USER,
    ADMIN;

    /**
     * Spring Security 권한 문자열.
     *
     * <p>★hasRole("ADMIN")은 내부적으로 "ROLE_ADMIN"을 찾는다★
     * 접두사를 빼먹으면 인가가 조용히 실패한다(403인데 원인이 안 보인다).
     */
    public String authority() {
        return "ROLE_" + name();
    }
}
