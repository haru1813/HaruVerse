package com.haru.haruverse.external.starrail;

import com.haru.haruverse.global.exception.ExternalApiException;

/** 스타레일 리소스 조회 실패 — 공통 부모를 상속해 502로 처리된다 */
public class StarRailApiException extends ExternalApiException {
    public StarRailApiException(String message, Throwable cause) {
        super("StarRail", message, cause);
    }
}
