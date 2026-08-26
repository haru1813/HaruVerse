package com.haru.haruverse.external.starrail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru.haruverse.external.starrail.dto.StarRailCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * 붕괴: 스타레일 캐릭터 리소스 조회.
 *
 * <p><b>왜 게임사 공식 API가 아닌가</b>
 * 호요버스는 캐릭터 도감용 공개 API를 제공하지 않는다.
 * 커뮤니티가 정리해 둔 공개 저장소(StarRailRes)를 쓴다.
 * 키가 필요 없고 정적 파일이라 요청 제한도 없다.
 *
 * <p>⚠️ 이미지·이름의 저작권은 호요버스에 있다. 검색·열람 목적으로 표시만 한다
 * (이용약관 제3조와 같은 취급).
 */
@Component
public class StarRailClient {

    private static final Logger log = LoggerFactory.getLogger(StarRailClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public StarRailClient(RestClient.Builder builder,
                          ObjectMapper objectMapper,
                          @Value("${external.starrail.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    /**
     * 캐릭터 전체 목록.
     *
     * <p>응답이 <b>배열이 아니라 id를 키로 하는 객체</b>다.
     * {@code {"1001": {...}, "1002": {...}}} → Map으로 받아 값만 꺼낸다.
     */
    public List<StarRailCharacter> fetchCharacters() {
        try {
            String body = restClient.get()
                    .uri("/index_min/en/characters.json")
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) return List.of();

            Map<String, StarRailCharacter> byId =
                    objectMapper.readValue(body, new TypeReference<>() {});

            log.info("스타레일 캐릭터 {}명 조회", byId.size());
            return List.copyOf(byId.values());

        } catch (RestClientException e) {
            throw new StarRailApiException("스타레일 리소스 조회에 실패했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StarRailApiException("스타레일 응답을 해석하지 못했습니다: " + e.getMessage(), e);
        }
    }

    /** 상대 경로(icon/character/1001.png)를 실제 URL로 */
    public String toImageUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        return baseUrl + "/" + relativePath;
    }
}
