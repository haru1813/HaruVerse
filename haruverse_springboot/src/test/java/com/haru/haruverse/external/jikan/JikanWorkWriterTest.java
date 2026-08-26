package com.haru.haruverse.external.jikan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.haruverse.external.jikan.dto.JikanAnime;
import com.haru.haruverse.external.jikan.dto.JikanPageResponse;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * upsert 로직 테스트 — 같은 작품을 두 번 수집해도 중복 저장되지 않고 갱신되는지 검증.
 * (external_id UNIQUE 제약이 실제로 지켜지는지가 핵심)
 */
@SpringBootTest
@Transactional
class JikanWorkWriterTest {

    @Autowired JikanWorkWriter writer;
    @Autowired WorkRepository workRepository;
    @Autowired ObjectMapper objectMapper;

    private JikanAnime sampleAnime() throws Exception {
        JikanPageResponse res = objectMapper.readValue(
                new ClassPathResource("jikan/top-anime-sample.json").getInputStream(),
                JikanPageResponse.class);
        return res.safeData().get(0); // Frieren
    }

    @Test
    @DisplayName("최초 수집 → 새 작품이 생성된다 (반환값 true)")
    void upsert_create() throws Exception {
        JikanAnime anime = sampleAnime();

        boolean created = writer.upsert(anime);

        assertThat(created).isTrue();
        Optional<Work> saved = workRepository.findByExternalId("jikan-52991");
        assertThat(saved).isPresent();

        Work work = saved.get();
        assertThat(work.getTitle()).isEqualTo("Frieren: Beyond Journey's End"); // 영문 제목 우선
        assertThat(work.getType()).isEqualTo(WorkType.ANIME);
        assertThat(work.getSource()).isEqualTo(WorkSource.JIKAN);
        assertThat(work.getSeason()).isEqualTo("2023-fall");
        assertThat(work.getReleaseDate()).isEqualTo(LocalDate.of(2023, 9, 29)); // aired.from 파싱
        assertThat(work.getRating()).isEqualByComparingTo(new BigDecimal("9.3")); // 9.26 → 소수1자리 반올림
        assertThat(work.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 작품 재수집 → 새로 만들지 않고 갱신한다 (건수 그대로, 반환값 false)")
    void upsert_update() throws Exception {
        JikanAnime anime = sampleAnime();

        writer.upsert(anime);              // 1회차
        long countAfterFirst = workRepository.count();

        boolean created = writer.upsert(anime); // 2회차 — 같은 mal_id

        assertThat(created).isFalse();
        assertThat(workRepository.count()).isEqualTo(countAfterFirst); // 중복 저장 안 됨
    }

    @Test
    @DisplayName("재수집 시 바뀐 평점이 반영된다")
    void upsert_updatesChangedValue() throws Exception {
        JikanAnime anime = sampleAnime();
        writer.upsert(anime);

        // 평점만 바뀐 응답이 왔다고 가정 (record라 불변 → 새 인스턴스로 재구성)
        JikanAnime changed = new JikanAnime(
                anime.malId(), anime.title(), anime.titleEnglish(), anime.type(),
                7.1, anime.season(), anime.year(), anime.synopsis(),
                anime.images(), anime.aired(), anime.studios(), anime.genres());

        writer.upsert(changed);

        Work work = workRepository.findByExternalId("jikan-52991").orElseThrow();
        assertThat(work.getRating()).isEqualByComparingTo(new BigDecimal("7.1"));
    }

    @Test
    @DisplayName("영문 제목이 없으면 원제(title)를 쓴다")
    void upsert_fallbackTitle() throws Exception {
        JikanAnime anime = sampleAnime();
        JikanAnime noEnglish = new JikanAnime(
                999999L, "원제만있음", null, anime.type(), anime.score(),
                anime.season(), anime.year(), anime.synopsis(),
                anime.images(), anime.aired(), anime.studios(), anime.genres());

        writer.upsert(noEnglish);

        Work work = workRepository.findByExternalId("jikan-999999").orElseThrow();
        assertThat(work.getTitle()).isEqualTo("원제만있음");
    }
}
