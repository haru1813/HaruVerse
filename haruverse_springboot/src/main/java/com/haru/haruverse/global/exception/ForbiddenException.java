package com.haru.haruverse.global.exception;

/**
 * 로그인은 했지만 그 일을 할 권한이 없을 때 → 403 Forbidden.
 *
 * <p><b>401과 구분해야 한다</b>
 * 401은 "누구인지 모른다"(로그인 필요), 403은 "누구인지는 알지만 안 된다".
 * 남의 글을 지우려는 요청에 401을 주면 클라이언트가 로그인 화면으로 보내버리는데,
 * 이미 로그인한 사용자라 무한히 반복된다.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
