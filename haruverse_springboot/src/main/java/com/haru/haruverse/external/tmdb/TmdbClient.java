package com.haru.haruverse.external.tmdb;

import com.haru.haruverse.external.tmdb.dto.TmdbSearchResponse;
import com.haru.haruverse.global.exception.MissingApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * TMDB API 호출.
 *
 * <p><b>이 프로젝트에서 TMDB 를 쓰는 목적은 하나 — 한국어 제목이다.</b>
 * Jikan·RAWG 가 주는 제목은 전부 영문이라, 한국인이 "프리렌"으로 검색하면 아무것도 안 나온다.
 * 검색 엔진을 바꿔서 풀 수 있는 문제가 아니라 <b>데이터가 없는 문제</b>다.
 * TMDB 는 {@code language=ko-KR} 로 현지화 제목을 준다.
 *
 * <p><b>애니는 TV 시리즈로 등록돼 있다.</b> 극장판은 영화 쪽이라, 둘 다 찾아본다.
 * 게임은 TMDB 에 없으므로 대상이 아니다.
 */
@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public TmdbClient(RestClient tmdbRestClient,
                      @Value("${external.tmdb.api-key:}") String apiKey) {
        this.restClient = tmdbRestClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    /** 키가 설정돼 있는지 — 없으면 수집 API 가 503 을 준다 */
    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    /** TV 시리즈 검색 (애니 대부분이 여기 있다) */
    public TmdbSearchResponse searchTv(String query, String language) {
        return search("/search/tv", query, language);
    }

    /** 영화 검색 (극장판) */
    public TmdbSearchResponse searchMovie(String query, String language) {
        return search("/search/movie", query, language);
    }

    /**
     * TV 상세 — 한국어 제목을 받으러 부른다.
     *
     * <p><b>★검색과 상세를 나눠 부르는 이유★</b>
     * 검색을 {@code ko-KR} 로 하면 {@code name} 은 한국어인데 {@code original_name} 은
     * <b>일본어</b>다(葬送のフリーレン). 우리가 가진 건 영문 제목이라 어느 쪽과도 비교가 안 된다.
     * 실제로 이 방식으로 처음 돌렸을 때 원제가 라틴 문자인 작품만 매칭됐다.
     * → 매칭은 {@code en-US} 로, 한국어 제목은 상세 조회로 따로 받는다.
     */
    public TmdbSearchResponse.Result fetchTvDetail(long id, String language) {
        return detail("/tv/%d".formatted(id), language);
    }

    /** 영화 상세 — 위와 같은 이유 */
    public TmdbSearchResponse.Result fetchMovieDetail(long id, String language) {
        return detail("/movie/%d".formatted(id), language);
    }

    private TmdbSearchResponse.Result detail(String path, String language) {
        if (!hasApiKey()) {
            throw new MissingApiKeyException("TMDB");
        }
        String uri = "%s?api_key=%s&language=%s".formatted(path, apiKey, language);
        try {
            // 상세 응답에도 name·title·original_* 이 그대로 있어 Result 로 받을 수 있다
            return restClient.get().uri(uri).retrieve().body(TmdbSearchResponse.Result.class);
        } catch (Exception e) {
            log.warn("TMDB 상세 호출 실패: {} — {}", path, e.toString());
            throw new TmdbApiException("TMDB 상세 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private TmdbSearchResponse search(String path, String query, String language) {
        if (!hasApiKey()) {
            throw new MissingApiKeyException("TMDB");
        }
        // ★제목에 &, ?, 공백이 흔하다★ 인코딩하지 않으면 쿼리스트링이 잘려
        //   엉뚱한 검색이 되거나 400 이 난다. (예: "Fate/stay night")
        String uri = "%s?api_key=%s&query=%s&language=%s&include_adult=false"
                .formatted(path, apiKey, encode(query), language);
        try {
            return restClient.get().uri(uri).retrieve().body(TmdbSearchResponse.class);
        } catch (Exception e) {
            // ★로그에 키를 남기지 않는다★ uri 를 그대로 찍으면 키가 로그 파일에 박힌다
            log.warn("TMDB 호출 실패: {} query=\"{}\" — {}", path, query, e.toString());
            throw new TmdbApiException("TMDB API 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
