package com.haru.haruverse.external.jikan;

import com.haru.haruverse.global.exception.ExternalApiException;

/**
 * Jikan API 호출 실패.
 *
 * <p>공통 {@link ExternalApiException}을 상속하므로
 * 예외 처리는 GlobalExceptionHandler 한 곳에서 일괄로 이뤄진다.
 */
public class JikanApiException extends ExternalApiException {
    public JikanApiException(String message, Throwable cause) {
        super("Jikan", message, cause);
    }
}
