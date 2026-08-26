package com.haru.haruverse.work;

import com.haru.haruverse.genre.entity.Genre;
import com.haru.haruverse.genre.repository.GenreRepository;
import com.haru.haruverse.studio.entity.Studio;
import com.haru.haruverse.studio.repository.StudioRepository;
import com.haru.haruverse.studio.service.StudioService;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import com.haru.haruverse.work.service.WorkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 작품 목록 필터 조합.
 *
 * <p><b>이 테스트가 생긴 이유</b>
 * 예전 구현은 if 분기로 조건 하나를 골라 파생 메서드를 불렀다.
 * 검색어가 있으면 맨 위 분기에서 끝나 type·genre가 무시됐고,
 * 그래서 <b>게임 탭에서 검색하면 애니가 나왔다.</b>
 * 조건이 함께 걸리는지 조합별로 확인한다.
 */
@SpringBootTest
class WorkFilterTest {

    @Autowired WorkService workService;
    @Autowired WorkRepository workRepository;
    @Autowired GenreRepository genreRepository;
    @Autowired StudioRepository studioRepository;
    @Autowired StudioService studioService;

    private static final String STUDIO_A = "필터테스트 스튜디오A";
    private static final String STUDIO_B = "필터테스트 스튜디오B";
    private static final String GENRE_X = "필터테스트장르X";
    private static final String GENRE_Y = "필터테스트장르Y";

    @BeforeEach
    void setUp() {
        // 이 테스트가 만든 데이터만 지운다 (다른 테스트의 작품은 건드리지 않는다)
        workRepository.findAll().stream()
                .filter(w -> w.getTitle().startsWith("필터테스트"))
                .forEach(workRepository::delete);

        Studio a = studioService.findOrCreate(STUDIO_A);
        Studio b = studioService.findOrCreate(STUDIO_B);
        Genre x = genreRepository.findByName(GENRE_X).orElseGet(() -> genreRepository.save(new Genre(GENRE_X)));
        Genre y = genreRepository.findByName(GENRE_Y).orElseGet(() -> genreRepository.save(new Genre(GENRE_Y)));

        save("필터테스트 애니 알파", WorkType.ANIME, WorkSource.JIKAN, a, x, "2026-spring");
        save("필터테스트 애니 베타", WorkType.ANIME, WorkSource.JIKAN, a, y, "2026-summer");
        save("필터테스트 애니 감마", WorkType.ANIME, WorkSource.JIKAN, b, x, "2026-spring");
        save("필터테스트 게임 알파", WorkType.GAME, WorkSource.RAWG, b, x, null);
        save("필터테스트 게임 델타", WorkType.GAME, WorkSource.RAWG, null, y, null);
    }

    private void save(String title, WorkType type, WorkSource source, Studio studio, Genre genre, String season) {
        Work w = new Work(title, type, source);
        w.withDetails(null, LocalDate.of(2026, 1, 1), season, null, null, null);
        if (studio != null) w.assignStudio(studio);
        if (genre != null) w.replaceGenres(java.util.Set.of(genre));
        workRepository.save(w);
    }

    private java.util.List<String> titles(WorkType type, String season, String genre, String keyword, String studio) {
        return workService.getWorks(type, season, genre, keyword, studio, PageRequest.of(0, 50))
                .content().stream()
                .map(w -> w.title())
                .filter(t -> t.startsWith("필터테스트"))
                .toList();
    }

    @Test
    @DisplayName("★회귀★ 검색어 + type — 게임 탭에서 검색하면 게임만 나온다")
    void keywordWithType() {
        // 예전에는 keyword 분기가 먼저라 type이 무시되어 애니가 섞여 나왔다
        assertThat(titles(WorkType.GAME, null, null, "필터테스트", null))
                .containsExactlyInAnyOrder("필터테스트 게임 알파", "필터테스트 게임 델타");
    }

    @Test
    @DisplayName("★회귀★ 검색어 + 장르 — 둘 다 걸린다")
    void keywordWithGenre() {
        assertThat(titles(null, null, GENRE_Y, "알파", null)).isEmpty(); // 알파 중 Y장르는 없다
        assertThat(titles(null, null, GENRE_X, "알파", null))
                .containsExactlyInAnyOrder("필터테스트 애니 알파", "필터테스트 게임 알파");
    }

    @Test
    @DisplayName("제작사로 거른다")
    void byStudio() {
        assertThat(titles(null, null, null, null, STUDIO_A))
                .containsExactlyInAnyOrder("필터테스트 애니 알파", "필터테스트 애니 베타");
    }

    @Test
    @DisplayName("제작사 이름은 대소문자를 가리지 않는다")
    void studioIgnoreCase() {
        assertThat(titles(null, null, null, null, STUDIO_A.toUpperCase())).hasSize(2);
    }

    @Test
    @DisplayName("제작사 + type 조합")
    void studioWithType() {
        assertThat(titles(WorkType.ANIME, null, null, null, STUDIO_B))
                .containsExactly("필터테스트 애니 감마");
        assertThat(titles(WorkType.GAME, null, null, null, STUDIO_B))
                .containsExactly("필터테스트 게임 알파");
    }

    @Test
    @DisplayName("제작사 + 장르 + 검색어를 한 번에")
    void allTogether() {
        assertThat(titles(WorkType.ANIME, "2026-spring", GENRE_X, "필터테스트", STUDIO_A))
                .containsExactly("필터테스트 애니 알파");
    }

    @Test
    @DisplayName("조건이 없으면 전체가 나온다")
    void noFilter() {
        assertThat(titles(null, null, null, "필터테스트", null)).hasSize(5);
    }

    @Test
    @DisplayName("제작사가 없는 작품은 studio 조건에서 제외된다")
    void workWithoutStudio() {
        // '게임 델타'는 제작사가 없다 — inner join이라 자연히 빠진다
        assertThat(titles(null, null, null, null, STUDIO_B)).doesNotContain("필터테스트 게임 델타");
    }

    /**
     * ★테스트가 만든 데이터를 반드시 지운다★
     *
     * <p>이 테스트들은 트랜잭션 롤백에 기대지 않으므로(서비스의 트랜잭션 경계를 실제로 보려고)
     * 남긴 작품이 그대로 DB에 있는다. 그러면 전체 목록을 검증하는 다른 테스트
     * (WorkControllerTest 등)가 이 데이터를 보고 실패한다 — 실제로 그랬다.
     */
    @AfterEach
    void tearDown() {
        workRepository.findAll().stream()
                .filter(w -> w.getTitle().startsWith("필터테스트"))
                .forEach(workRepository::delete);
    }
}
