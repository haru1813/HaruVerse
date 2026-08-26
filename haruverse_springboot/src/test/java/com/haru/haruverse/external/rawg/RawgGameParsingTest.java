package com.haru.haruverse.external.rawg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.haruverse.external.rawg.dto.RawgGame;
import com.haru.haruverse.external.rawg.dto.RawgPageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAWG 응답 파싱 테스트.
 *
 * <p>픽스처는 <b>실제 API 응답</b>을 잘라 저장한 것이다(2026-08-25 수집).
 * API 키는 마스킹해서 저장했다.
 * 네트워크를 타지 않으므로 RAWG가 죽어 있어도 테스트는 항상 돌아간다.
 */
@SpringBootTest
class RawgGameParsingTest {

    @Autowired
    ObjectMapper objectMapper;

    private RawgPageResponse loadList() throws Exception {
        return objectMapper.readValue(
                new ClassPathResource("rawg/games-sample.json").getInputStream(),
                RawgPageResponse.class);
    }

    private RawgGame loadDetail() throws Exception {
        return objectMapper.readValue(
                new ClassPathResource("rawg/game-detail-sample.json").getInputStream(),
                RawgGame.class);
    }

    @Test
    @DisplayName("snake_case JSON이 record 필드로 매핑된다 (background_image → backgroundImage)")
    void parseFields() throws Exception {
        RawgGame first = loadList().safeResults().get(0);

        assertThat(first.id()).isEqualTo(25097L);
        assertThat(first.name()).isEqualTo("The Legend of Zelda: Ocarina of Time");
        assertThat(first.released()).isEqualTo("1998-11-21");
        assertThat(first.backgroundImage()).startsWith("https://media.rawg.io/");
        assertThat(first.metacritic()).isEqualTo(99);
        assertThat(first.genres()).extracting(RawgGame.Named::name)
                .containsExactly("Action", "Adventure", "RPG");
    }

    @Test
    @DisplayName("선언하지 않은 필드(tags·stores·screenshots 등 20여 개)가 있어도 파싱에 실패하지 않는다")
    void ignoresUnknownFields() throws Exception {
        String json = """
                {"count":1,"next":null,"results":[
                  {"id":1,"name":"테스트","tags":[{"id":9,"name":"x"}],
                   "stores":[{"store":{"id":1}}],"dominant_color":"0f0f0f"}]}
                """;
        RawgPageResponse res = objectMapper.readValue(json, RawgPageResponse.class);
        assertThat(res.safeResults().get(0).name()).isEqualTo("테스트");
    }

    /* ── 평점 스케일 통일 ─────────────────────────────── */

    @Test
    @DisplayName("★스케일 통일★ metacritic(0~100)이 있으면 /10 → 10점 만점")
    void rating10_prefersMetacritic() throws Exception {
        RawgGame zelda = loadList().safeResults().get(0);   // metacritic 99, rating 4.37
        // 애니(Jikan)가 0~10이므로 게임도 10점 만점으로 맞춘다.
        // rating(4.37)×2=8.74 가 아니라 metacritic 쪽을 쓴다 (표본이 크고 신뢰도 높음)
        assertThat(zelda.rating10()).isEqualTo(9.9);
    }

    @Test
    @DisplayName("metacritic이 없으면 rating(0~5)에 2를 곱해 10점 만점으로")
    void rating10_fallbackToRating() throws Exception {
        String json = """
                {"results":[{"id":1,"name":"T","metacritic":null,"rating":4.05}]}
                """;
        RawgGame g = objectMapper.readValue(json, RawgPageResponse.class).safeResults().get(0);
        assertThat(g.rating10()).isEqualTo(8.1);
    }

    @Test
    @DisplayName("평점이 아예 없으면 null (0점으로 저장하지 않는다)")
    void rating10_null() throws Exception {
        String json = """
                {"results":[{"id":1,"name":"T","metacritic":null,"rating":0.0}]}
                """;
        RawgGame g = objectMapper.readValue(json, RawgPageResponse.class).safeResults().get(0);
        assertThat(g.rating10()).isNull();
    }

    /* ── 페이징 ──────────────────────────────────────── */

    @Test
    @DisplayName("next가 있으면 hasNext() true (Jikan의 has_next_page와 형태가 다름)")
    void pagination() throws Exception {
        assertThat(loadList().hasNext()).isTrue();
    }

    /* ── 상세 응답 ───────────────────────────────────── */

    @Test
    @DisplayName("상세 응답에는 목록에 없는 description_raw·developers가 있다")
    void parseDetail() throws Exception {
        RawgGame detail = loadDetail();

        assertThat(detail.descriptionRaw()).isNotBlank();
        assertThat(detail.developers()).extracting(RawgGame.Named::name).contains("Nintendo");
        assertThat(detail.studioName()).isEqualTo("Nintendo");
    }

    @Test
    @DisplayName("개발사가 없으면 배급사를 제작사로 쓴다")
    void studioName_fallback() throws Exception {
        String json = """
                {"results":[{"id":1,"name":"T","developers":[],
                 "publishers":[{"id":9,"name":"Sony","slug":"sony"}]}]}
                """;
        RawgGame g = objectMapper.readValue(json, RawgPageResponse.class).safeResults().get(0);
        assertThat(g.studioName()).isEqualTo("Sony");
    }
}
