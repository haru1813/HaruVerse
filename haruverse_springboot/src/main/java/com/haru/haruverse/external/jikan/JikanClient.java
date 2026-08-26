package com.haru.haruverse.external.jikan;

import java.util.List;

import com.haru.haruverse.external.jikan.dto.JikanAnime;
import com.haru.haruverse.external.jikan.dto.JikanCharacterEntry;
import com.haru.haruverse.external.jikan.dto.JikanCharacterListResponse;
import com.haru.haruverse.external.jikan.dto.JikanPageResponse;
import com.haru.haruverse.external.jikan.dto.JikanSingleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Jikan API(MyAnimeList 비공식 API) 호출 담당.
 *
 * <p>여기서는 <b>HTTP 호출과 파싱만</b> 한다. DB 저장·변환은 JikanCollectService의 일.
 * 이렇게 나누면 수집 로직을 테스트할 때 이 클래스만 가짜로 바꿔 끼울 수 있다.
 *
 * <p>⚠️ Jikan 요청 제한: 초당 3회 / 분당 60회.
 * 여러 페이지를 연속으로 긁을 때는 호출부에서 간격을 둬야 한다.
 */
@Component
public class JikanClient {

    private static final Logger log = LoggerFactory.getLogger(JikanClient.class);

    private final RestClient restClient;

    // RestClientConfig에서 baseUrl·타임아웃이 설정된 빈을 주입받는다
    public JikanClient(RestClient jikanRestClient) {
        this.restClient = jikanRestClient;
    }

    /**
     * 인기 애니 목록 — GET /top/anime?page={page}&limit={limit}
     *
     * @param page  1부터 시작 (Jikan은 0이 아니라 1부터)
     * @param limit 한 페이지 건수 (최대 25)
     */
    public JikanPageResponse fetchTopAnime(int page, int limit) {
        return get("/top/anime?page=%d&limit=%d".formatted(page, limit));
    }

    /**
     * 분기별 애니 목록 — GET /seasons/{year}/{season}?page={page}&limit={limit}
     *
     * @param season spring / summer / fall / winter
     */
    public JikanPageResponse fetchSeasonAnime(int year, String season, int page, int limit) {
        return get("/seasons/%d/%s?page=%d&limit=%d".formatted(year, season, page, limit));
    }

    /**
     * 애니 단건 조회 — GET /anime/{malId}
     *
     * <p>목록 응답과 달리 data가 배열이 아니라 객체라 별도 DTO를 쓴다.
     * 특정 작품만 다시 갱신하거나, 목록 API가 불안정할 때의 우회 경로로도 쓰인다.
     */
    public JikanAnime fetchAnime(long malId) {
        JikanSingleResponse res = get("/anime/" + malId, JikanSingleResponse.class);
        return res == null ? null : res.data();
    }

    /**
     * 작품의 등장인물 — GET /anime/{malId}/characters
     *
     * <p>페이징이 없다. 한 작품에 60건 넘게 오는 경우도 있다(프리렌 63건).
     * 주역은 서너 명뿐이고 나머지는 전부 조역이다.
     */
    public List<JikanCharacterEntry> fetchCharacters(long malId) {
        JikanCharacterListResponse res =
                get("/anime/%d/characters".formatted(malId), JikanCharacterListResponse.class);
        return res == null ? List.of() : res.safeData();
    }

    private JikanPageResponse get(String uri) {
        JikanPageResponse res = get(uri, JikanPageResponse.class);
        log.debug("Jikan 호출 성공: {} ({}건)", uri, res == null ? 0 : res.safeData().size());
        return res;
    }

    private <T> T get(String uri, Class<T> type) {
        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(type);

        } catch (RestClientException e) {
            // 외부 API 장애(504 등)를 그대로 500으로 흘려보내지 않고 우리 예외로 감싼다.
            // → 호출부가 "외부 API 문제"임을 구분해 처리할 수 있다.
            log.warn("Jikan 호출 실패: {} — {}", uri, e.getMessage());
            throw new JikanApiException("Jikan API 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
