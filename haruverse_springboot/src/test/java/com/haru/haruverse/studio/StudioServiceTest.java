package com.haru.haruverse.studio;

import com.haru.haruverse.studio.dto.StudioResponse;
import com.haru.haruverse.studio.entity.Studio;
import com.haru.haruverse.studio.service.StudioService;
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
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 제작사 목록 집계.
 *
 * <p>다른 테스트가 만든 작품도 DB에 있으므로, 이 테스트가 만든 것만 골라 확인한다.
 */
@SpringBootTest
class StudioServiceTest {

    @Autowired StudioService studioService;
    @Autowired WorkRepository workRepository;

    private static final String BIG = "집계테스트 큰스튜디오";
    private static final String SMALL = "집계테스트 작은스튜디오";
    private static final String EMPTY = "집계테스트 작품없는스튜디오";

    @BeforeEach
    void setUp() {
        workRepository.findAll().stream()
                .filter(w -> w.getTitle().startsWith("집계테스트"))
                .forEach(workRepository::delete);

        Studio big = studioService.findOrCreate(BIG);
        Studio small = studioService.findOrCreate(SMALL);
        studioService.findOrCreate(EMPTY); // 작품을 붙이지 않는다

        save("집계테스트 작품1", big);
        save("집계테스트 작품2", big);
        save("집계테스트 작품3", big);
        save("집계테스트 작품4", small);
    }

    private void save(String title, Studio studio) {
        Work w = new Work(title, WorkType.ANIME, WorkSource.MANUAL);
        w.assignStudio(studio);
        workRepository.save(w);
    }

    private List<StudioResponse> mine(String keyword) {
        return studioService.getStudios(keyword, PageRequest.of(0, 100)).content().stream()
                .filter(s -> s.name().startsWith("집계테스트"))
                .toList();
    }

    @Test
    @DisplayName("작품 수가 함께 집계된다")
    void countsWorks() {
        assertThat(mine(null))
                .extracting(StudioResponse::name, StudioResponse::workCount)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(BIG, 3L),
                        org.assertj.core.groups.Tuple.tuple(SMALL, 1L));
    }

    @Test
    @DisplayName("작품이 많은 순으로 정렬된다")
    void orderedByWorkCount() {
        List<StudioResponse> list = mine(null);
        assertThat(list).extracting(StudioResponse::name).containsSubsequence(BIG, SMALL);
    }

    @Test
    @DisplayName("★작품이 0편인 제작사는 목록에 나오지 않는다★ (inner join)")
    void excludesEmptyStudio() {
        assertThat(mine(null)).extracting(StudioResponse::name).doesNotContain(EMPTY);
    }

    @Test
    @DisplayName("이름으로 검색된다 (대소문자 무시)")
    void searchByName() {
        assertThat(mine("큰스튜디오")).extracting(StudioResponse::name).containsExactly(BIG);
    }

    @Test
    @DisplayName("검색어가 없으면 전체가 나온다 (null 조건이 무시된다)")
    void noKeyword() {
        assertThat(mine(null)).hasSize(2);
        assertThat(mine("  ")).hasSize(2); // 공백만 있는 검색어도 전체
    }

    @Test
    @DisplayName("총 개수가 그룹 수와 맞는다 (count 쿼리가 행 수를 세지 않는다)")
    void totalElementsMatchesGroupCount() {
        var page = studioService.getStudios("집계테스트", PageRequest.of(0, 100));
        // BIG(3편)+SMALL(1편) = 작품 4행이지만, 제작사는 2개다.
        // countQuery를 따로 주지 않았다면 여기서 4가 나온다.
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).hasSize(2);
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
                .filter(w -> w.getTitle().startsWith("집계테스트"))
                .forEach(workRepository::delete);
    }
}
