package com.haru.haruverse.external.tmdb;

import com.haru.haruverse.global.exception.ExternalApiException;

/** TMDB API 호출 실패 (요청 한도 초과·서버 장애 등) */
public class TmdbApiException extends ExternalApiException {
    public TmdbApiException(String message, Throwable cause) {
        super("TMDB", message, cause);
    }
}
