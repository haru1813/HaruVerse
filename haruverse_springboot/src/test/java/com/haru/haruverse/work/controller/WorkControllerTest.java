package com.haru.haruverse.work.controller;

import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.service.WorkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WorkController 통합 테스트 — 인증 없이 접근 가능한지, 필터·페이징·404가 맞는지 검증.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    WorkService workService;

    private Long animeId;

    @BeforeEach
    void setUp() {
        Work anime = workService.save(new Work("괴수 8호", WorkType.ANIME, WorkSource.JIKAN)
                .withDetails("괴수를 청소하던 남자", LocalDate.of(2026, 4, 13),
                        "2026-spring", new BigDecimal("8.3"), "https://img/1.jpg", "ctrl-jikan-1"));
        animeId = anime.getId();

        workService.save(new Work("젤다의 전설", WorkType.GAME, WorkSource.RAWG)
                .withDetails("하이랄 모험", LocalDate.of(2023, 5, 12),
                        null, new BigDecimal("9.6"), "https://img/3.jpg", "ctrl-rawg-1"));
    }

    @Test
    @DisplayName("GET /api/works: 토큰 없이도 200 (공개 API) + 페이징 포맷")
    void getWorks_withoutToken() throws Exception {
        mockMvc.perform(get("/api/works"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /api/works?type=GAME: 종류로 필터링된다")
    void getWorks_filterByType() throws Exception {
        mockMvc.perform(get("/api/works").param("type", "GAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("젤다의 전설"))
                .andExpect(jsonPath("$.content[0].type").value("GAME"));
    }

    @Test
    @DisplayName("GET /api/works?q=괴수: 제목 부분 검색")
    void getWorks_searchByKeyword() throws Exception {
        mockMvc.perform(get("/api/works").param("q", "괴수"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("괴수 8호"));
    }

    @Test
    @DisplayName("GET /api/works?type=FOO: enum에 없는 값 → 400")
    void getWorks_invalidType() throws Exception {
        mockMvc.perform(get("/api/works").param("type", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/works/{id}: 상세 조회 — 목록에 없는 synopsis까지 내려온다")
    void getWork_detail() throws Exception {
        mockMvc.perform(get("/api/works/{id}", animeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("괴수 8호"))
                .andExpect(jsonPath("$.synopsis").value("괴수를 청소하던 남자"))
                .andExpect(jsonPath("$.source").value("JIKAN"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("GET /api/works/{id}: 없는 id → 404")
    void getWork_notFound() throws Exception {
        mockMvc.perform(get("/api/works/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
