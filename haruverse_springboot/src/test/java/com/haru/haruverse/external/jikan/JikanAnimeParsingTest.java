package com.haru.haruverse.external.jikan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.haruverse.external.jikan.dto.JikanAnime;
import com.haru.haruverse.external.jikan.dto.JikanPageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jikan 응답 파싱 테스트 — 실제 API 응답을 잘라 저장한 픽스처로 검증한다.
 *
 * <p>네트워크를 타지 않으므로 외부 API가 죽어 있어도(504) 테스트는 항상 돌아간다.
 * 외부 의존을 테스트에서 끊어내는 게 핵심.
 */
@SpringBootTest
class JikanAnimeParsingTest {

    @Autowired
    ObjectMapper objectMapper; // 스프링이 쓰는 것과 동일한 설정의 매퍼로 검증

    private JikanPageResponse load() throws Exception {
        return objectMapper.readValue(
                new ClassPathResource("jikan/top-anime-sample.json").getInputStream(),
                JikanPageResponse.class);
    }

    @Test
    @DisplayName("snake_case JSON이 record 필드로 매핑된다 (mal_id → malId 등)")
    void parseFields() throws Exception {
        JikanPageResponse res = load();

        assertThat(res.safeData()).hasSize(2);
        JikanAnime first = res.safeData().get(0);

        assertThat(first.malId()).isEqualTo(52991L);
        assertThat(first.title()).isEqualTo("Sousou no Frieren");
        assertThat(first.titleEnglish()).isEqualTo("Frieren: Beyond Journey's End");
        assertThat(first.score()).isEqualTo(9.26);
        assertThat(first.year()).isEqualTo(2023);
        assertThat(first.season()).isEqualTo("fall");
    }

    @Test
    @DisplayName("중첩 객체(images.jpg / aired.from)와 배열(studios·genres)도 매핑된다")
    void parseNested() throws Exception {
        JikanAnime first = load().safeData().get(0);

        assertThat(first.posterUrl()).startsWith("https://cdn.myanimelist.net/");
        assertThat(first.aired().from()).isEqualTo("2023-09-29T00:00:00+00:00");
        assertThat(first.studios()).extracting(JikanAnime.Named::name).contains("Madhouse");
        assertThat(first.genres()).extracting(JikanAnime.Named::name).contains("Fantasy");
    }

    @Test
    @DisplayName("seasonKey(): year + season을 ERD 형식('2023-fall')으로 조합한다")
    void seasonKey() throws Exception {
        assertThat(load().safeData().get(0).seasonKey()).isEqualTo("2023-fall");
    }

    @Test
    @DisplayName("pagination.has_next_page → hasNext()")
    void pagination() throws Exception {
        assertThat(load().hasNext()).isTrue();
    }

    @Test
    @DisplayName("선언하지 않은 필드(rank·members 등 20여 개)가 있어도 파싱에 실패하지 않는다")
    void ignoresUnknownFields() throws Exception {
        // 픽스처는 이미 잘라냈으므로, 없는 필드를 일부러 넣어 확인
        String json = """
                {"pagination":{"has_next_page":false},
                 "data":[{"mal_id":1,"title":"테스트","이상한필드":123,"nested":{"a":1}}]}
                """;
        JikanPageResponse res = objectMapper.readValue(json, JikanPageResponse.class);

        assertThat(res.safeData()).hasSize(1);
        assertThat(res.safeData().get(0).title()).isEqualTo("테스트");
    }
}
