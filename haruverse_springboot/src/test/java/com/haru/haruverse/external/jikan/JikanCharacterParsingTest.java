package com.haru.haruverse.external.jikan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.haruverse.character.entity.CharacterRole;
import com.haru.haruverse.external.jikan.dto.JikanCharacterEntry;
import com.haru.haruverse.external.jikan.dto.JikanCharacterListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캐릭터 응답 파싱 테스트.
 *
 * <p>픽스처는 <b>실제 API 응답</b>(GET /anime/52991/characters, 2026-08-25)에서
 * 5건을 추린 것이다. 네트워크를 타지 않으므로 MAL이 죽어 있어도 항상 돌아간다.
 */
@SpringBootTest
class JikanCharacterParsingTest {

    @Autowired
    ObjectMapper objectMapper;

    private List<JikanCharacterEntry> load() throws Exception {
        return objectMapper.readValue(
                new ClassPathResource("jikan/anime-characters-sample.json").getInputStream(),
                JikanCharacterListResponse.class).safeData();
    }

    private JikanCharacterEntry find(String name) throws Exception {
        return load().stream().filter(e -> name.equals(e.name())).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("중첩 구조(character.images.jpg.image_url)가 편의 메서드로 평평해진다")
    void parseFields() throws Exception {
        JikanCharacterEntry frieren = find("Frieren");

        assertThat(frieren.malId()).isEqualTo(184947L);
        assertThat(frieren.imageUrl()).startsWith("https://cdn.myanimelist.net/images/characters/");
        assertThat(frieren.role()).isEqualTo("Main");
    }

    @Test
    @DisplayName("★favorites는 character 안이 아니라 바깥에 있다★")
    void favoritesIsOutsideCharacter() throws Exception {
        // 실수하기 쉬운 지점 — character.favorites 로 선언했다면 항상 null이 됐을 것
        assertThat(find("Frieren").favorites()).isEqualTo(32400);
    }

    @Test
    @DisplayName("성우 10개 언어 중 일본어만 골라낸다")
    void picksJapaneseVoiceActor() throws Exception {
        assertThat(find("Frieren").japaneseVoiceActor()).isEqualTo("Tanezaki, Atsumi");
        assertThat(find("Fern").japaneseVoiceActor()).isEqualTo("Ichinose, Kana");
    }

    @Test
    @DisplayName("일본어 성우가 둘 이상이면 첫 번째를 쓴다 (Stark)")
    void multipleJapaneseVoiceActors() throws Exception {
        // 실제 응답에 이런 캐릭터가 있다 — findFirst가 아니면 예외가 나거나 엉뚱한 값이 들어간다
        assertThat(find("Stark").japaneseVoiceActor()).isEqualTo("Kobayashi, Chiaki");
    }

    @Test
    @DisplayName("성우 정보가 아예 없으면 null (빈 문자열이 아니라)")
    void noVoiceActor() throws Exception {
        JikanCharacterEntry bakery = find("Bakery Owner");

        assertThat(bakery.voiceActors()).isEmpty();
        assertThat(bakery.japaneseVoiceActor()).isNull();
        assertThat(bakery.isValid()).isTrue(); // 성우가 없어도 저장은 된다
    }

    @Test
    @DisplayName("선언하지 않은 필드가 있어도 파싱이 깨지지 않는다")
    void ignoresUnknownFields() throws Exception {
        String json = """
                {"data":[{"character":{"mal_id":1,"name":"테스트","nicknames":["별명"],
                          "about":"설명"},"role":"Main","favorites":5,"anime":[]}]}
                """;
        var entry = objectMapper.readValue(json, JikanCharacterListResponse.class).safeData().get(0);
        assertThat(entry.name()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("role 문자열 → enum. 모르는 값은 SUPPORTING으로 (수집이 멈추지 않게)")
    void roleMapping() {
        assertThat(CharacterRole.from("Main")).isEqualTo(CharacterRole.MAIN);
        assertThat(CharacterRole.from("main")).isEqualTo(CharacterRole.MAIN);
        assertThat(CharacterRole.from("Supporting")).isEqualTo(CharacterRole.SUPPORTING);
        assertThat(CharacterRole.from("Cameo")).isEqualTo(CharacterRole.SUPPORTING);
        assertThat(CharacterRole.from(null)).isEqualTo(CharacterRole.SUPPORTING);
    }

    @Test
    @DisplayName("data가 null이어도 빈 리스트 (호출부에서 null 검사 불필요)")
    void safeData() throws Exception {
        var res = objectMapper.readValue("{}", JikanCharacterListResponse.class);
        assertThat(res.safeData()).isEmpty();
    }
}
