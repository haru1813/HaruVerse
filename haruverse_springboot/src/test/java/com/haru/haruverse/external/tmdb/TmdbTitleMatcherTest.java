package com.haru.haruverse.external.tmdb;

import com.haru.haruverse.external.tmdb.dto.TmdbSearchResponse.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TMDB 후보 고르기.
 *
 * <p><b>이 클래스가 이 기능의 정확도를 정한다.</b> 첫 결과를 그냥 쓰면
 * "Attack on Titan" 에 시즌 5의 한국어 제목이 붙는 식으로 조용히 틀린다.
 * 틀린 제목은 비어 있는 것보다 나쁘므로, <b>확신 없으면 포기</b>하는지를 여기서 고정한다.
 */
class TmdbTitleMatcherTest {

    private final TmdbTitleMatcher matcher = new TmdbTitleMatcher();

    /** TV 결과 하나 만들기 (name=한국어 제목, originalName=원제) */
    private Result tv(String korean, String original, String firstAirDate) {
        return new Result(1L, korean, original, firstAirDate, null, null, null, 10.0);
    }

    @Test
    @DisplayName("제목과 연도가 맞으면 고른다")
    void picksMatching() {
        Result hit = matcher.pick(
                List.of(tv("장송의 프리렌", "Frieren: Beyond Journey's End", "2023-09-29")),
                "Frieren: Beyond Journey's End", 2023);

        assertThat(hit).isNotNull();
        assertThat(hit.localizedTitle()).isEqualTo("장송의 프리렌");
    }

    @Test
    @DisplayName("★연도가 어긋나면 버린다★ — 시즌이 뒤바뀌는 걸 막는다")
    void rejectsWrongYear() {
        Result hit = matcher.pick(
                List.of(tv("진격의 거인 파이널 시즌", "Attack on Titan Final Season", "2020-12-07")),
                "Attack on Titan", 2013);

        assertThat(hit).isNull();
    }

    @Test
    @DisplayName("방영 시차 1년까지는 허용한다")
    void allowsOneYearGap() {
        Result hit = matcher.pick(
                List.of(tv("강철의 연금술사", "Fullmetal Alchemist: Brotherhood", "2010-04-05")),
                "Fullmetal Alchemist: Brotherhood", 2009);

        assertThat(hit).isNotNull();
    }

    @Test
    @DisplayName("★제목이 안 겹치면 버린다★ — 연도만 같은 남의 작품")
    void rejectsUnrelatedTitle() {
        Result hit = matcher.pick(
                List.of(tv("전혀 다른 작품", "Some Completely Different Show", "2023-01-01")),
                "Frieren: Beyond Journey's End", 2023);

        assertThat(hit).isNull();
    }

    @Test
    @DisplayName("★짧은 제목이 긴 제목에 포함돼도 통과시키지 않는다★")
    void rejectsSubstringMatch() {
        // 실제로 겪은 오탐: "Your Name." 이 한국 드라마 "Live Up to Your Name" 과
        // 맞아떨어져 "명불허전" 이 붙었다. 짧은 쪽 기준으로 나누면 1.0 이 된다.
        Result hit = matcher.pick(
                List.of(tv("명불허전", "Live Up to Your Name", "2017-08-12")),
                "Your Name.", 2016);

        assertThat(hit).isNull();
    }

    @Test
    @DisplayName("여러 후보 중 제목이 가장 잘 맞는 것을 고른다")
    void picksBestAmongCandidates() {
        Result hit = matcher.pick(List.of(
                        tv("스타인즈 게이트 제로", "Steins;Gate 0", "2018-04-12"),
                        tv("슈타인즈 게이트", "Steins;Gate", "2011-04-06")),
                "Steins;Gate", 2011);

        assertThat(hit).isNotNull();
        assertThat(hit.localizedTitle()).isEqualTo("슈타인즈 게이트");
    }

    @Test
    @DisplayName("후보가 없으면 null")
    void emptyCandidates() {
        assertThat(matcher.pick(List.of(), "무엇이든", 2020)).isNull();
        assertThat(matcher.pick(null, "무엇이든", 2020)).isNull();
    }

    @Test
    @DisplayName("우리 쪽 연도를 모르면 제목만으로 판단한다")
    void worksWithoutYear() {
        Result hit = matcher.pick(
                List.of(tv("카우보이 비밥", "Cowboy Bebop", "1998-04-03")),
                "Cowboy Bebop", null);

        assertThat(hit).isNotNull();
    }

    /* ── 유사도 자체 ─────────────────────────────────── */

    @Test
    @DisplayName("유사도는 긴 쪽 기준이다 (포함 관계만으로 만점이 되지 않게)")
    void similarityUsesLongerSide() {
        // "Frieren" 이 포함되지만 나머지 3단어가 다르므로 만점이 아니다
        assertThat(matcher.similarity("Frieren: Beyond Journey's End", "Frieren")).isEqualTo(0.25);
        // 완전히 같으면 1.0
        assertThat(matcher.similarity("Cowboy Bebop", "Cowboy Bebop")).isEqualTo(1.0);
        // 전혀 안 겹치면 0
        assertThat(matcher.similarity("Frieren", "Bleach")).isZero();
    }

    @Test
    @DisplayName("대소문자·기호는 무시한다")
    void ignoresCaseAndPunctuation() {
        assertThat(matcher.similarity("Steins;Gate", "steins gate")).isEqualTo(1.0);
    }
}
