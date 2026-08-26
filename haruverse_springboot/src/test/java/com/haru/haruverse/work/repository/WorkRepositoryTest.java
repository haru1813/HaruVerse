package com.haru.haruverse.work.repository;

import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// WorkRepository 슬라이스 테스트 — 쿼리 메서드(타입·분기·제목검색·externalId)가
// 실제 H2에서 의도대로 동작하는지 검증.
@DataJpaTest
class WorkRepositoryTest {

    @Autowired
    WorkRepository workRepository;

    @BeforeEach
    void setUp() {
        workRepository.save(new Work("괴수 8호", WorkType.ANIME, WorkSource.JIKAN)
                .withDetails("괴수를 청소하던 남자", LocalDate.of(2026, 4, 13),
                        "2026-spring", new BigDecimal("8.3"), "https://img/1.jpg", "jikan-52588"));

        workRepository.save(new Work("던전밥", WorkType.ANIME, WorkSource.JIKAN)
                .withDetails("던전에서 밥을 먹는다", LocalDate.of(2026, 1, 4),
                        "2026-winter", new BigDecimal("8.6"), "https://img/2.jpg", "jikan-52701"));

        workRepository.save(new Work("젤다의 전설", WorkType.GAME, WorkSource.RAWG)
                .withDetails("하이랄 모험", LocalDate.of(2023, 5, 12),
                        null, new BigDecimal("9.6"), "https://img/3.jpg", "rawg-58175"));
    }

    @Test
    @DisplayName("findByType: 종류로 거른다 (ANIME 2건 / GAME 1건)")
    void findByType() {
        Page<Work> anime = workRepository.findByType(WorkType.ANIME, PageRequest.of(0, 10));
        Page<Work> game = workRepository.findByType(WorkType.GAME, PageRequest.of(0, 10));

        assertThat(anime.getTotalElements()).isEqualTo(2);
        assertThat(game.getTotalElements()).isEqualTo(1);
        assertThat(game.getContent().get(0).getTitle()).isEqualTo("젤다의 전설");
    }

    @Test
    @DisplayName("findByTypeAndSeason: 종류 + 분기를 함께 거른다")
    void findByTypeAndSeason() {
        Page<Work> result = workRepository.findByTypeAndSeason(
                WorkType.ANIME, "2026-spring", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("괴수 8호");
    }

    @Test
    @DisplayName("findByTitleContainingIgnoreCase: 제목 부분 일치로 찾는다")
    void findByTitleContaining() {
        Page<Work> result = workRepository.findByTitleContainingIgnoreCase("던전", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("던전밥");
    }

    @Test
    @DisplayName("findByExternalId: 외부 API ID로 중복 수집 여부를 판단한다")
    void findByExternalId() {
        Optional<Work> found = workRepository.findByExternalId("jikan-52588");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("괴수 8호");
        assertThat(workRepository.existsByExternalId("없는-id")).isFalse();
    }

    @Test
    @DisplayName("BaseTimeEntity: 저장 시 createdAt·updatedAt이 자동으로 채워진다")
    void baseTimeEntity() {
        Work saved = workRepository.save(new Work("신작", WorkType.ANIME, WorkSource.MANUAL));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
