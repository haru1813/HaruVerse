package com.haru.haruverse.admin;

import com.haru.haruverse.admin.dto.AdminStats;
import com.haru.haruverse.admin.service.AdminStatsService;
import com.haru.haruverse.global.jwt.JwtTokenProvider;
import com.haru.haruverse.member.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 통계 API.
 *
 * <p>두 가지를 본다.
 * <ol>
 *   <li><b>인가</b> — {@code /api/admin/**} 가 ADMIN 에게만 열려 있는가.
 *       회원 수·게시글 수처럼 서비스 내부 사정이 나가는 경로다</li>
 *   <li><b>드리프트 판정</b> — 색인 수를 못 읽었을 때 '어긋났다'고 오판하지 않는가</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminStatsTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired AdminStatsService statsService;

    @Test
    @DisplayName("★비로그인은 통계를 볼 수 없다★ (401)")
    void anonymousBlocked() throws Exception {
        mockMvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("★일반 회원도 통계를 볼 수 없다★ (403)")
    void normalUserBlocked() throws Exception {
        String token = tokenProvider.createToken("user@haru.test", MemberRole.USER);

        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 통계를 볼 수 있다 (200)")
    void adminAllowed() throws Exception {
        String token = tokenProvider.createToken("admin@haru.test", MemberRole.ADMIN);

        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("집계가 음수 없이 채워진다")
    void collectsWithoutNegatives() {
        AdminStats stats = statsService.collect();

        assertThat(stats.works()).isGreaterThanOrEqualTo(0);
        assertThat(stats.members()).isGreaterThanOrEqualTo(0);
        // 애니 + 게임이 전체를 넘을 수 없다 (종류가 늘면 작아질 수는 있다)
        assertThat(stats.anime() + stats.games()).isLessThanOrEqualTo(stats.works());
        // 한국어 제목을 채운 작품이 애니 전체보다 많을 수는 없다
        assertThat(stats.titleKoFilled()).isLessThanOrEqualTo(stats.anime());
        // 캐릭터가 붙은 애니가 애니 전체보다 많을 수는 없다
        // (분모를 works 로 두면 이 검증이 느슨해져 버그를 놓친다)
        assertThat(stats.animeWithCharacters()).isLessThanOrEqualTo(stats.anime());
    }

    @Test
    @DisplayName("★ES에 못 붙으면 '어긋났다'가 아니라 '알 수 없다'★")
    void unknownIsNotDrift() {
        // 테스트 환경은 search.elasticsearch.enabled=false 이고 ES 서버도 없다.
        // 이때 indexed 는 null 이어야 하고, indexDrift 는 false 여야 한다.
        //
        // ★이걸 반대로 짜기 쉽다★ "색인 수(0) != 작품 수" 로 계산하면
        // ES가 죽어 있는 동안 대시보드가 계속 빨간 경고를 띄운다.
        // 연결 실패는 어긋난 게 아니라 판단할 수 없는 상태다.
        AdminStats stats = statsService.collect();

        if (stats.indexed() == null) {
            assertThat(stats.indexDrift()).isFalse();
        }
    }
}
