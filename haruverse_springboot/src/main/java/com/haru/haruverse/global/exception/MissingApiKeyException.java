package com.haru.haruverse.global.exception;

/**
 * 외부 API 키가 설정되지 않아 기능을 쓸 수 없는 상태.
 *
 * <p>클라이언트 잘못이 아니라 <b>서버 설정 미비</b>이므로 4xx가 아닌
 * 503 Service Unavailable로 응답한다. (409/500으로 내보내면 원인 파악이 어렵다)
 */
public class MissingApiKeyException extends RuntimeException {
    public MissingApiKeyException(String message) {
        super(message);
    }
}
