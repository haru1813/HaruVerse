package com.haru.haruverse.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 외부 API 호출용 RestClient 빈 설정.
 *
 * <p>RestClient는 Spring 6.1에서 들어온 동기 HTTP 클라이언트다.
 * (RestTemplate의 후속 — 유지보수 모드인 RestTemplate 대신 신규 코드에 권장)
 *
 * <p>★타임아웃을 반드시 건다★
 * 기본값은 '무제한'이라, 외부 API가 응답하지 않으면 우리 스레드가 영원히 붙잡힌다.
 * 요청이 몰리면 톰캣 스레드풀이 고갈되어 서비스 전체가 멈출 수 있다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient jikanRestClient(
            @Value("${external.jikan.base-url}") String baseUrl,
            @Value("${external.jikan.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${external.jikan.read-timeout-ms}") long readTimeoutMs) {

        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }

    @Bean
    public RestClient rawgRestClient(
            @Value("${external.rawg.base-url}") String baseUrl,
            @Value("${external.rawg.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${external.rawg.read-timeout-ms}") long readTimeoutMs) {

        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
