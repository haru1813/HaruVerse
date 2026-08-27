package com.haru.haruverse.search;

import com.haru.haruverse.genre.entity.Genre;
import com.haru.haruverse.search.document.WorkDocument;
import com.haru.haruverse.studio.entity.Studio;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 엔티티 → 검색 문서 변환.
 *
 * <p>Elasticsearch 없이 도는 순수 변환 테스트다. 색인이 실제로 되는지는
 * ES가 떠 있어야 하므로 여기서 보지 않고, <b>무엇을 담고 무엇을 안 담는지</b>만 고정한다.
 */
class WorkDocumentTest {

    private Work anime() {
        Work work = new Work("Frieren: Beyond Journey's End", WorkType.ANIME, WorkSource.JIKAN)
                .withDetails("장송의 프리렌", LocalDate.of(2023, 9, 29), "2023 가을",
                        BigDecimal.valueOf(9.3), "https://img/frieren.jpg", "jikan-52991");
        work.assignStudio(new Studio("Madhouse"));
        work.replaceGenres(Set.of(new Genre("Adventure")));
        return work;
    }

    @Test
    @DisplayName("작품의 검색 대상 필드가 문서로 옮겨진다")
    void convertsFields() {
        WorkDocument doc = WorkDocument.from(anime());

        assertThat(doc.getTitle()).isEqualTo("Frieren: Beyond Journey's End");
        assertThat(doc.getType()).isEqualTo("ANIME");
        assertThat(doc.getSeason()).isEqualTo("2023 가을");
        assertThat(doc.getStudio()).isEqualTo("Madhouse");
        assertThat(doc.getGenres()).containsExactly("Adventure");
        assertThat(doc.getRating()).isEqualTo(9.3);
        assertThat(doc.getReleaseDate()).isEqualTo("2023-09-29");
    }

    @Test
    @DisplayName("★한글 제목·별칭은 비어 있다★ — 채울 데이터 출처가 아직 없다")
    void koreanTitleIsEmptyForNow() {
        WorkDocument doc = WorkDocument.from(anime());

        // Jikan·RAWG가 주는 제목이 전부 영문이라 한글 제목이 한 건도 없다.
        // "프리렌"으로 Frieren을 찾는 건 검색 엔진이 아니라 데이터 문제다.
        // 필드는 미리 열어두었으므로, 출처가 생기면 여기만 채우면 된다.
        assertThat(doc.getTitleKo()).isNull();
        assertThat(doc.getAliases()).isEmpty();
    }

    @Test
    @DisplayName("제작사가 없어도 변환에 실패하지 않는다")
    void nullStudioIsFine() {
        Work work = new Work("무소속 작품", WorkType.GAME, WorkSource.MANUAL);

        WorkDocument doc = WorkDocument.from(work);

        assertThat(doc.getStudio()).isNull();
        assertThat(doc.getGenres()).isEmpty();
        assertThat(doc.getPlatforms()).isEmpty();
        assertThat(doc.getRating()).isNull();
        assertThat(doc.getReleaseDate()).isNull();
    }

    @Test
    @DisplayName("게임 플랫폼이 문서에 담긴다")
    void gamePlatforms() {
        Work game = new Work("Baldur's Gate III", WorkType.GAME, WorkSource.RAWG);
        game.replacePlatforms(Set.of("PC", "PlayStation"));

        assertThat(WorkDocument.from(game).getPlatforms())
                .containsExactlyInAnyOrder("PC", "PlayStation");
    }

    @Test
    @DisplayName("type 문자열을 WorkType으로 되돌린다")
    void workTypeRoundTrip() {
        assertThat(WorkDocument.from(anime()).workType()).isEqualTo(WorkType.ANIME);
        assertThat(new WorkDocument().workType()).isNull();
    }
}
