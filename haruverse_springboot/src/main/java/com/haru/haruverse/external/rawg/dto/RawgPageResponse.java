package com.haru.haruverse.external.rawg.dto;

import java.util.List;

/**
 * RAWG 목록 응답의 껍데기.
 *
 * <pre>
 * { "count": 878667, "next": "https://...page=2", "previous": null,
 *   "results": [ { ...게임... }, ... ] }
 * </pre>
 *
 * <p>Jikan은 pagination 객체 안에 has_next_page가 있었지만,
 * RAWG는 next에 다음 페이지 URL이 들어오고 마지막이면 null이다.
 */
public record RawgPageResponse(
        Integer count,
        String next,
        String previous,
        List<RawgGame> results
) {
    /** results가 null로 오는 경우(에러 응답 등) 대비 */
    public List<RawgGame> safeResults() {
        return results == null ? List.of() : results;
    }

    public boolean hasNext() {
        return next != null && !next.isBlank();
    }
}
