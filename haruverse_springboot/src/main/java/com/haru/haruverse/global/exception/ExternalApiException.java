package com.haru.haruverse.global.exception;

/**
 * 외부 API 호출 실패를 나타내는 공통 예외.
 *
 * <p>Jikan(애니)·RAWG(게임)처럼 연동 대상이 늘어나도
 * GlobalExceptionHandler는 이 타입 하나만 잡으면 된다(→ 502 Bad Gateway).
 *
 * <p>어느 API에서 났는지는 {@link #getApiName()}으로 구분한다.
 */
public class ExternalApiException extends RuntimeException {

    private final String apiName;

    public ExternalApiException(String apiName, String message, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
    }

    public String getApiName() {
        return apiName;
    }
}
