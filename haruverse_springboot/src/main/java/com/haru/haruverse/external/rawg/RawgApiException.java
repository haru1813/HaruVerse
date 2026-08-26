package com.haru.haruverse.external.rawg;

import com.haru.haruverse.global.exception.ExternalApiException;

/** RAWG API 호출 실패 (키 누락·요청 한도 초과·서버 장애 등) */
public class RawgApiException extends ExternalApiException {
    public RawgApiException(String message, Throwable cause) {
        super("RAWG", message, cause);
    }
}
