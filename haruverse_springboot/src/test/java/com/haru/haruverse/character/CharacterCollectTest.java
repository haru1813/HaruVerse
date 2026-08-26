package com.haru.haruverse.character;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.haruverse.character.entity.AnimeCharacter;
import com.haru.haruverse.character.entity.CharacterRole;
import com.haru.haruverse.character.repository.AnimeCharacterRepository;
import com.haru.haruverse.character.repository.WorkCharacterRepository;
import com.haru.haruverse.character.service.CharacterService;
import com.haru.haruverse.external.jikan.JikanCharacterWriter;
import com.haru.haruverse.external.jikan.dto.JikanCharacterEntry;
import com.haru.haruverse.external.jikan.dto.JikanCharacterListResponse;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캐릭터 저장·조회 통합 테스트.
 *
 * <p>@Transactional을 붙이지 않는다 — 붙이면 테스트가 한 트랜잭션으로 묶여
 * 실제 커밋에서만 드러나는 문제를 놓친다.
 */
@SpringBootTest
class CharacterCollectTest {

    @Autowired JikanCharacterWriter writer;
    @Autowired CharacterService characterService;
    @Autowired AnimeCharacterRepository characterRepository;
    @Autowired WorkCharacterRepository workCharacterRepository;
    @Autowired WorkRepository workRepository;
    @Autowired ObjectMapper objectMapper;

    private Long workId;
    private Long otherWorkId;
    private List<JikanCharacterEntry> entries;

    @BeforeEach
    void setUp() throws Exception {
        // 연결이 캐릭터를 참조하므로 연결부터 지운다
        workCharacterRepository.deleteAll();
        characterRepository.deleteAll();

        workId = workRepository.save(new Work("캐릭터 테스트 작품", WorkType.ANIME, WorkSource.JIKAN)).getId();
        otherWorkId = workRepository.save(new Work("같은 캐릭터가 나오는 2기", WorkType.ANIME, WorkSource.JIKAN)).getId();

        entries = objectMapper.readValue(
                new ClassPathResource("jikan/anime-characters-sample.json").getInputStream(),
                JikanCharacterListResponse.class).safeData();
    }

    @Test
    @DisplayName("캐릭터와 연결이 저장된다")
    void save() {
        var r = writer.upsert(workId, entries);

        assertThat(r.created()).isEqualTo(5);
        assertThat(r.linked()).isEqualTo(5);
        assertThat(characterRepository.count()).isEqualTo(5);
        assertThat(workCharacterRepository.countByWorkId(workId)).isEqualTo(5);
    }

    @Test
    @DisplayName("★재수집해도 중복이 생기지 않는다★ (같은 작품을 두 번 수집)")
    void reCollectIsIdempotent() {
        writer.upsert(workId, entries);
        var second = writer.upsert(workId, entries);

        assertThat(second.created()).isZero();   // 새로 만든 캐릭터 없음
        assertThat(second.updated()).isEqualTo(5); // 전부 기존 것
        assertThat(second.linked()).isZero();    // 연결도 추가 없음

        assertThat(characterRepository.count()).isEqualTo(5);
        assertThat(workCharacterRepository.countByWorkId(workId)).isEqualTo(5);
    }

    @Test
    @DisplayName("다른 작품에 같은 캐릭터가 나오면 캐릭터는 재사용하고 연결만 늘어난다")
    void sharedCharacterAcrossWorks() {
        writer.upsert(workId, entries);
        var r = writer.upsert(otherWorkId, entries);

        assertThat(r.created()).isZero();      // 캐릭터는 이미 있다
        assertThat(r.linked()).isEqualTo(5);   // 연결만 새로 생긴다

        assertThat(characterRepository.count()).isEqualTo(5); // 10이 아니다
        assertThat(workCharacterRepository.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("성우가 없는 캐릭터도 저장된다 (voiceActor만 null)")
    void characterWithoutVoiceActor() {
        writer.upsert(workId, entries);

        AnimeCharacter bakery = characterRepository.findByExternalId("jikan-298195").orElseThrow();
        assertThat(bakery.getName()).isEqualTo("Bakery Owner");
        assertThat(bakery.getVoiceActor()).isNull();
    }

    @Test
    @DisplayName("작품의 등장인물은 주역 먼저, 그 안에서 인기순")
    void worksCharactersAreOrdered() {
        writer.upsert(workId, entries);

        var list = characterService.getCharactersOfWork(workId);

        // 앞 3명이 MAIN, 그 뒤가 SUPPORTING
        assertThat(list).extracting("role")
                .containsExactly("MAIN", "MAIN", "MAIN", "SUPPORTING", "SUPPORTING");
        // MAIN 안에서는 favorites 내림차순 (Frieren 32400 > Fern 5877 > Stark 3762)
        assertThat(list.subList(0, 3)).extracting("name")
                .containsExactly("Frieren", "Fern", "Stark");
    }

    @Test
    @DisplayName("캐릭터 목록은 인기순으로 나온다")
    void listByPopularity() {
        writer.upsert(workId, entries);

        var page = characterService.getCharacters(null, PageRequest.of(0, 24));

        assertThat(page.content()).extracting("name")
                .containsExactly("Frieren", "Fern", "Stark", "Aura", "Bakery Owner");
    }

    @Test
    @DisplayName("이름으로 검색된다 (대소문자 무시)")
    void searchByName() {
        writer.upsert(workId, entries);

        var page = characterService.getCharacters("frier", PageRequest.of(0, 24));
        assertThat(page.content()).extracting("name").containsExactly("Frieren");
    }

    @Test
    @DisplayName("캐릭터 상세에 출연 작품이 함께 나온다")
    void detailWithAppearances() {
        writer.upsert(workId, entries);
        writer.upsert(otherWorkId, entries);

        Long frierenId = characterRepository.findByExternalId("jikan-184947").orElseThrow().getId();
        var detail = characterService.getCharacter(frierenId);

        assertThat(detail.name()).isEqualTo("Frieren");
        assertThat(detail.appearances()).hasSize(2);
        assertThat(detail.appearances()).extracting("role").containsOnly(CharacterRole.MAIN.name());
    }
}
