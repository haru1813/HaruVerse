package com.haru.haruverse.voiceactor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.haruverse.character.repository.AnimeCharacterRepository;
import com.haru.haruverse.character.repository.WorkCharacterRepository;
import com.haru.haruverse.character.service.CharacterService;
import com.haru.haruverse.external.jikan.JikanCharacterWriter;
import com.haru.haruverse.external.jikan.dto.JikanCharacterEntry;
import com.haru.haruverse.external.jikan.dto.JikanCharacterListResponse;
import com.haru.haruverse.voiceactor.dto.VoiceActorResponse;
import com.haru.haruverse.voiceactor.entity.VoiceActor;
import com.haru.haruverse.voiceactor.repository.VoiceActorRepository;
import com.haru.haruverse.voiceactor.service.VoiceActorService;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.junit.jupiter.api.AfterEach;
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
 * 성우 도메인.
 *
 * <p>픽스처는 실제 Jikan 응답에서 추린 5건이다.
 * 성우가 없는 캐릭터(Bakery Owner)와 일본어 성우가 둘인 캐릭터(Stark)가 섞여 있다.
 */
@SpringBootTest
class VoiceActorTest {

    @Autowired JikanCharacterWriter writer;
    @Autowired VoiceActorService voiceActorService;
    @Autowired CharacterService characterService;
    @Autowired VoiceActorRepository voiceActorRepository;
    @Autowired AnimeCharacterRepository characterRepository;
    @Autowired WorkCharacterRepository workCharacterRepository;
    @Autowired WorkRepository workRepository;
    @Autowired ObjectMapper objectMapper;

    private Long workId;
    private List<JikanCharacterEntry> entries;

    @BeforeEach
    void setUp() throws Exception {
        // 삭제 순서 주의 — 연결 → 캐릭터 → 성우 (참조하는 쪽부터)
        workCharacterRepository.deleteAll();
        characterRepository.deleteAll();
        voiceActorRepository.deleteAll();

        workId = workRepository.save(new Work("성우테스트 작품", WorkType.ANIME, WorkSource.JIKAN)).getId();
        entries = objectMapper.readValue(
                new ClassPathResource("jikan/anime-characters-sample.json").getInputStream(),
                JikanCharacterListResponse.class).safeData();
    }

    @AfterEach
    void tearDown() {
        // ★삭제 순서★ work_character가 work를 참조하므로 연결을 먼저 지운다.
        //   순서를 어기면 커밋 때 FK 위반으로 터진다 (실제로 그랬다)
        workCharacterRepository.deleteAll();
        workRepository.findAll().stream()
                .filter(w -> w.getTitle().startsWith("성우테스트"))
                .forEach(workRepository::delete);
    }

    @Test
    @DisplayName("수집하면 성우가 엔티티로 저장된다 (식별자·이미지 포함)")
    void savesVoiceActorEntity() {
        writer.upsert(workId, entries);

        VoiceActor tanezaki = voiceActorRepository.findByName("Tanezaki, Atsumi").orElseThrow();
        assertThat(tanezaki.getMalId()).isNotNull();
        assertThat(tanezaki.getImageUrl()).startsWith("https://cdn.myanimelist.net/");
    }

    @Test
    @DisplayName("★같은 성우는 한 번만 만들어진다★ (캐릭터마다 새로 만들지 않는다)")
    void reusesVoiceActor() {
        writer.upsert(workId, entries);
        long first = voiceActorRepository.count();

        // 다른 작품에 같은 캐릭터들이 또 나와도 성우는 늘지 않는다
        Long other = workRepository.save(new Work("성우테스트 2기", WorkType.ANIME, WorkSource.JIKAN)).getId();
        writer.upsert(other, entries);

        assertThat(voiceActorRepository.count()).isEqualTo(first);
    }

    @Test
    @DisplayName("성우가 없는 캐릭터는 성우 없이 저장된다")
    void characterWithoutVoiceActor() {
        writer.upsert(workId, entries);

        var bakery = characterRepository.findByExternalId("jikan-298195").orElseThrow();
        assertThat(bakery.getVoiceActor()).isNull();
        assertThat(bakery.getVoiceActorName()).isNull();
    }

    @Test
    @DisplayName("성우 목록은 맡은 캐릭터가 많은 순")
    void listOrderedByCharacterCount() {
        writer.upsert(workId, entries);

        List<VoiceActorResponse> list = voiceActorService
                .getVoiceActors(null, PageRequest.of(0, 50)).content();

        assertThat(list).isNotEmpty();
        for (int i = 1; i < list.size(); i++) {
            assertThat(list.get(i).characterCount())
                    .isLessThanOrEqualTo(list.get(i - 1).characterCount());
        }
        // 픽스처에서 성우가 있는 캐릭터는 4명 → 성우도 4명
        assertThat(list).hasSize(4);
    }

    @Test
    @DisplayName("★담당 캐릭터가 없는 성우는 목록에 나오지 않는다★ (inner join)")
    void excludesVoiceActorWithoutCharacters() {
        voiceActorRepository.save(new VoiceActor("성우테스트 무직성우"));

        assertThat(voiceActorService.getVoiceActors(null, PageRequest.of(0, 50)).content())
                .extracting(VoiceActorResponse::name)
                .doesNotContain("성우테스트 무직성우");
    }

    @Test
    @DisplayName("성우 상세에 맡은 캐릭터가 인기순으로 나온다")
    void detailWithCharacters() {
        writer.upsert(workId, entries);

        Long id = voiceActorRepository.findByName("Tanezaki, Atsumi").orElseThrow().getId();
        var detail = voiceActorService.getVoiceActor(id);

        assertThat(detail.name()).isEqualTo("Tanezaki, Atsumi");
        assertThat(detail.characters()).extracting("name").containsExactly("Frieren");
    }

    @Test
    @DisplayName("이름으로 검색된다 (대소문자 무시)")
    void searchByName() {
        writer.upsert(workId, entries);

        assertThat(voiceActorService.getVoiceActors("tanezaki", PageRequest.of(0, 50)).content())
                .extracting(VoiceActorResponse::name)
                .containsExactly("Tanezaki, Atsumi");
    }

    @Test
    @DisplayName("캐릭터 응답에 성우 id가 실린다 (화면에서 성우로 이동하기 위해)")
    void characterResponseCarriesVoiceActorId() {
        writer.upsert(workId, entries);

        var list = characterService.getCharactersOfWork(workId);
        var frieren = list.stream().filter(c -> "Frieren".equals(c.name())).findFirst().orElseThrow();
        var bakery = list.stream().filter(c -> "Bakery Owner".equals(c.name())).findFirst().orElseThrow();

        assertThat(frieren.voiceActorId()).isNotNull();
        assertThat(frieren.voiceActor()).isEqualTo("Tanezaki, Atsumi");
        // 성우가 없으면 둘 다 null (빈 문자열이나 0이 아니다)
        assertThat(bakery.voiceActorId()).isNull();
        assertThat(bakery.voiceActor()).isNull();
    }

    @Test
    @DisplayName("이미 있는 성우에게 비어 있던 식별자·이미지를 채운다 (이관 데이터 보강 경로)")
    void fillsMissingFields() {
        // 이름만 있는 상태 = 이관으로 만들어진 성우
        voiceActorRepository.save(new VoiceActor("Tanezaki, Atsumi"));

        writer.upsert(workId, entries);

        VoiceActor v = voiceActorRepository.findByName("Tanezaki, Atsumi").orElseThrow();
        assertThat(v.getMalId()).isNotNull();
        assertThat(v.getImageUrl()).isNotNull();
        assertThat(voiceActorRepository.findAll()).filteredOn(x -> "Tanezaki, Atsumi".equals(x.getName()))
                .hasSize(1); // 중복 생성되지 않았다
    }
}
