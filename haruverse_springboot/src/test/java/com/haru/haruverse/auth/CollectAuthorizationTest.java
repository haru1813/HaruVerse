package com.haru.haruverse.auth;

import com.haru.haruverse.global.jwt.JwtTokenProvider;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수집 API 인가 — {@code /api/collect/**} 는 ADMIN만.
 *
 * <p><b>왜 이 테스트가 필요한가</b>
 * 수집 엔드포인트는 Jikan·RAWG를 <b>대신 호출</b>한다. 로그인만 하면 누구나 부를 수 있게
 * 두면, 가입한 아무나 외부 API 쿼터를 소진시키고 DB를 오염시킬 수 있다.
 * 공개 배포 전에 반드시 닫아야 했던 구멍이라, 다시 열리면 여기서 걸리게 한다.
 *
 * <p>실제로 수집이 되는지는 보지 않는다(외부 API를 부르면 테스트가 네트워크에 묶인다).
 * <b>인가 단계에서 막히는지만</b> 확인하므로 401·403이면 충분하다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CollectAuthorizationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider tokenProvider;

    /** 수집 엔드포인트 대표 4종 — 하나라도 빠지면 그 경로만 열려버린다 */
    private static final String[] COLLECT_PATHS = {
            "/api/collect/jikan/top",
            "/api/collect/jikan/characters",
            "/api/collect/rawg/games",
            "/api/collect/starrail/characters",
    };

    @Test
    @DisplayName("★비로그인은 수집 API에 접근할 수 없다★ (401)")
    void anonymousBlocked() throws Exception {
        for (String path : COLLECT_PATHS) {
            mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("★일반 회원도 수집 API에 접근할 수 없다★ (403) — 이게 이번에 막은 구멍")
    void normalUserBlocked() throws Exception {
        String token = tokenProvider.createToken("user@haru.test", MemberRole.USER);

        for (String path : COLLECT_PATHS) {
            mockMvc.perform(post(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("관리자는 인가를 통과한다 (401·403이 아니다)")
    void adminPasses() throws Exception {
        String token = tokenProvider.createToken("admin@haru.test", MemberRole.ADMIN);

        // ★수집이 실제로 일어나는 경로를 쓰면 안 된다★
        //   예전에는 /api/collect/jikan/top 을 불렀는데, 그게 진짜로 Jikan 을 호출하고
        //   가져온 작품을 커밋했다(이 클래스에는 @Transactional 이 없다).
        //   Jikan 인기 1위가 Frieren 이라 jikan-52991 이 테스트 DB 에 남았고,
        //   뒤에 도는 JikanWorkWriterTest 의 "최초 수집 → 생성(true)" 이 false 로 뒤집혔다.
        //   테스트 순서에 따라 통과하기도 해서 더 고약했다.
        //
        //   여기서 보려는 건 "인가에서 막히지 않는다" 하나뿐이다.
        //   필수 파라미터(ids)를 빼면 인가를 통과한 뒤 400 에서 멈춘다 —
        //   네트워크도 안 타고 DB 도 건드리지 않는다.
        int statusCode = mockMvc.perform(post("/api/collect/jikan/ids")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();

        assertThat(statusCode).isNotIn(401, 403);
    }

    /* ── 토큰의 role 클레임 ───────────────────────────── */

    @Test
    @DisplayName("토큰에 권한이 실리고 그대로 읽힌다")
    void roleRoundTrip() {
        String admin = tokenProvider.createToken("a@haru.test", MemberRole.ADMIN);
        String user = tokenProvider.createToken("u@haru.test", MemberRole.USER);

        assertThat(tokenProvider.getRole(admin)).isEqualTo(MemberRole.ADMIN);
        assertThat(tokenProvider.getRole(user)).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("★hasRole은 ROLE_ 접두사를 찾는다★ — 빼먹으면 인가가 조용히 실패한다")
    void authorityPrefix() {
        assertThat(MemberRole.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
        assertThat(MemberRole.USER.authority()).isEqualTo("ROLE_USER");
    }

    /* ── 가입 기본값 ──────────────────────────────────── */

    @Test
    @DisplayName("새로 가입하면 USER 다 (관리자는 DB에서 직접 승격)")
    void newMemberIsUser() {
        assertThat(new Member("x@haru.test", "encoded", "새회원").getRole())
                .isEqualTo(MemberRole.USER);
    }
}
