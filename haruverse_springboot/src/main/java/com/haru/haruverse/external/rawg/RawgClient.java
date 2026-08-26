package com.haru.haruverse.external.rawg;

import com.haru.haruverse.external.rawg.dto.RawgGame;
import com.haru.haruverse.external.rawg.dto.RawgPageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.haru.haruverse.global.exception.MissingApiKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * RAWG API(게임 데이터베이스) 호출 담당.
 *
 * <p>Jikan과 달리 <b>API 키가 필수</b>다(없으면 401).
 * 키는 소스에 두지 않고 환경변수 RAWG_API_KEY로 주입받는다.
 *
 * <p>무료 티어 한도: 월 20,000 요청.
 */
@Component
public class RawgClient {

    private static final Logger log = LoggerFactory.getLogger(RawgClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public RawgClient(RestClient rawgRestClient,
                      @Value("${external.rawg.api-key:}") String apiKey) {
        this.restClient = rawgRestClient;
        this.apiKey = apiKey;
    }

    /** 키가 설정돼 있는지 — 컨트롤러에서 미리 확인해 친절한 메시지를 주려고 공개 */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 게임 목록 — GET /games?key=...&page=..&page_size=..&ordering=..
     *
     * @param ordering 정렬 (예: "-rating", "-metacritic", "-released")
     */
    public RawgPageResponse fetchGames(int page, int pageSize, String ordering) {
        String uri = "/games?key=%s&page=%d&page_size=%d&ordering=%s"
                .formatted(key(), page, pageSize, enc(ordering));
        return get(uri, RawgPageResponse.class);
    }

    /**
     * 게임 단건 — GET /games/{id}?key=...
     *
     * <p>목록에는 없는 description_raw·developers가 여기에만 있다.
     */
    public RawgGame fetchGame(long gameId) {
        return get("/games/%d?key=%s".formatted(gameId, key()), RawgGame.class);
    }

    private String key() {
        if (!hasApiKey()) {
            throw new MissingApiKeyException(
                    "RAWG API 키가 설정되지 않았습니다. 환경변수 RAWG_API_KEY를 지정해주세요.");
        }
        return apiKey;
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private <T> T get(String uri, Class<T> type) {
        try {
            return restClient.get().uri(uri).retrieve().body(type);
        } catch (RestClientException e) {
            // 키를 로그에 남기지 않도록 쿼리스트링에서 제거
            log.warn("RAWG 호출 실패: {} — {}", maskKey(uri), e.getMessage());
            throw new RawgApiException("RAWG API 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /** 로그에 API 키가 찍히지 않게 마스킹 */
    private String maskKey(String uri) {
        return uri.replaceAll("key=[^&]*", "key=***");
    }
}
