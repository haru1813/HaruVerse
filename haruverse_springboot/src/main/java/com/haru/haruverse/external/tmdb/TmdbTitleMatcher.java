package com.haru.haruverse.external.tmdb;

import com.haru.haruverse.external.tmdb.dto.TmdbSearchResponse.Result;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * TMDB 검색 결과 중 <b>우리 작품과 같은 것</b>을 고른다.
 *
 * <p><b>★첫 결과를 그냥 쓰면 안 된다★</b>
 * "Frieren" 을 검색하면 본편·시즌2·특별편이 다 나온다. "Attack on Titan" 은 시즌이 다섯이다.
 * 인기순 1위를 집으면 시즌이 어긋나고, 심하면 아예 다른 작품이 붙는다.
 * <b>틀린 한글 제목이 붙는 것보다 비어 있는 게 낫다</b> — 사용자는 틀린 제목을 보고
 * "이 사이트 데이터가 엉망"이라고 판단하지, "매칭이 실패했구나"라고 생각하지 않는다.
 *
 * <p>그래서 <b>두 관문</b>을 통과한 것만 채택한다.
 * <ol>
 *   <li><b>연도</b> — 출시 연도가 ±1년 안이어야 한다(국가별 방영 시차 감안)</li>
 *   <li><b>제목</b> — 원제가 우리 제목과 충분히 겹쳐야 한다</li>
 * </ol>
 * 둘 다 만족하는 후보가 없으면 <b>포기한다</b>(null).
 */
@Component
public class TmdbTitleMatcher {

    /** 방영 시차를 감안한 연도 허용 오차 */
    private static final int YEAR_TOLERANCE = 1;
    /**
     * 제목 일치로 인정할 최소 비율.
     *
     * <p>0.6 이었을 때 {@code "Code Geass: Lelouch of the Rebellion R2"} 가
     * <b>특별판 OVA</b>(단어 6개 공유 / 10개)와 정확히 0.6 으로 통과했다.
     * TMDB 에 R2 가 별도 작품으로 없어 그게 최선의 후보였던 건데,
     * 그래도 다른 판본이므로 채택하면 안 된다. 한 단계 올려 걸러낸다.
     */
    private static final double MIN_TITLE_SIMILARITY = 0.65;

    /**
     * 후보 중 가장 그럴듯한 하나를 고른다.
     *
     * @param ourTitle 우리 DB의 영문 제목
     * @param ourYear  우리 DB의 출시 연도 (없으면 null — 그러면 제목만으로 판단)
     * @return 채택된 후보. 확신이 없으면 <b>null</b>
     */
    public Result pick(List<Result> candidates, String ourTitle, Integer ourYear) {
        if (candidates == null || candidates.isEmpty()) return null;

        Result best = null;
        double bestScore = 0;

        for (Result c : candidates) {
            if (c.localizedTitle() == null || c.localizedTitle().isBlank()) continue;

            // ── 관문 ① 연도 ──
            if (ourYear != null && c.year() != null
                    && Math.abs(ourYear - c.year()) > YEAR_TOLERANCE) {
                continue;
            }

            // ── 관문 ② 제목 ──
            //   ★영문 이름과 원제를 둘 다 보고 더 높은 쪽을 쓴다★
            //   en-US 로 검색하므로 localizedTitle 이 영문이다. 다만 원제가 라틴 문자인
            //   작품(STEINS;GATE 등)은 original 쪽이 더 잘 맞는 경우가 있다.
            double score = Math.max(
                    similarity(ourTitle, c.localizedTitle()),
                    similarity(ourTitle, c.original()));
            if (score < MIN_TITLE_SIMILARITY) continue;

            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    /**
     * 제목 유사도 — 단어 단위로 얼마나 겹치는지.
     *
     * <p>편집 거리(Levenshtein)를 쓰지 않은 이유: 제목은 기호·표기 차이가 잦은데
     * ("Steins;Gate" vs "steins gate") 단어 집합으로 보면 그게 다 흡수된다.
     *
     * <p><b>★긴 쪽 기준으로 나눈다★</b>
     * 처음엔 짧은 쪽 기준이었다. 부제가 붙거나 빠지는 경우를 잡으려던 건데,
     * <b>짧은 제목이 긴 제목에 통째로 포함되면 무조건 1.0</b>이 되는 게 문제였다.
     * 실제로 {@code "Your Name."} 이 한국 드라마 {@code "Live Up to Your Name"} 과
     * 1.0 으로 맞아 <b>"명불허전"</b> 이 붙었다.
     *
     * <p>en-US 로 검색하면 TMDB 가 영문명을 그대로 주므로, 짧은 쪽 기준의 관대함이
     * 애초에 필요 없다. 긴 쪽 기준이면 그 오탐이 0.4 로 떨어져 걸러진다.
     */
    double similarity(String a, String b) {
        if (a == null || b == null) return 0;
        var wordsA = words(a);
        var wordsB = words(b);
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0;

        long shared = wordsA.stream().filter(wordsB::contains).count();
        return (double) shared / Math.max(wordsA.size(), wordsB.size());
    }

    /**
     * 소문자화 + 기호 제거 후 단어로 쪼갠다 ("Frieren: Beyond" → [frieren, beyond]).
     *
     * <p>★한 글자 토큰은 버린다★ "Journey's" 를 기호 제거로 쪼개면 {@code journey} 와
     * {@code s} 가 나오는데, 그 {@code s} 가 분모를 키워 유사도를 깎는다.
     * 의미 없는 조각이라 세지 않는다.
     */
    private List<String> words(String text) {
        return List.of(text.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9가-힣\\s]", " ")
                        .trim()
                        .split("\\s+"))
                .stream()
                .filter(w -> w.length() > 1)
                .toList();
    }
}
