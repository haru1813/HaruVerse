package com.haru.haruverse.global.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;

// 전역 예외 처리 — 컨트롤러에서 던진 예외를 일관된 JSON 응답으로 변환.
// (프론트는 { "message": "..." } 형태를 읽어 화면에 표시)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 응답 바디 형태
    public record ErrorResponse(String message) {}

    // 로그인은 했지만 권한이 없음 → 403 Forbidden
    //
    // ★401과 구분★ 401은 "누구인지 모른다", 403은 "누구인지는 알지만 안 된다".
    // 남의 글 삭제 시도에 401을 주면 클라이언트가 로그인 화면으로 보내는데,
    // 이미 로그인한 사용자라 그 순환이 끝나지 않는다.
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
    }

    // 한 번에 처리할 수 있는 개수 초과 → 400 Bad Request
    @ExceptionHandler(TooManyItemsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyItems(TooManyItemsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
    }

    // DB 제약(유니크 등) 위반 → 409 Conflict
    //
    // ★언제 나는가★ 같은 작품을 동시에 두 번 찜하는 경우처럼,
    // 서비스의 exists 검사와 INSERT 사이 틈으로 중복이 들어왔을 때 DB가 막아준다.
    // 서비스에서 이 예외를 잡아 삼키면 안 된다 — INSERT가 터진 시점에 트랜잭션이
    // rollback-only로 마킹되어, 잡아도 커밋 때 UnexpectedRollbackException이 다시 난다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        // 원인 메시지에는 테이블·컬럼명이 그대로 담기므로 클라이언트에 노출하지 않는다
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("이미 처리된 요청입니다."));
    }

    // 회원가입 이메일 중복 등 '잘못된 상태' → 409 Conflict
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
    }

    // 로그인 실패(이메일/비밀번호 불일치) 등 '잘못된 인자' → 401 Unauthorized
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
    }

    // 리소스 없음(작품 조회 실패 등) → 404 Not Found
    // ※ IllegalArgumentException을 쓰면 위 핸들러 때문에 401이 나가므로 별도 예외를 쓴다.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
    }

    // 외부 API(Jikan·RAWG 등) 장애 → 502 Bad Gateway
    // 우리 서버가 잘못한 게 아니라 '상류 서버'가 문제라는 뜻이므로 500이 아닌 502가 맞다.
    // 공통 부모 타입을 잡으므로 연동 대상이 늘어나도 이 핸들러 하나면 된다.
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternalApi(ExternalApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(e.getApiName() + " API를 사용할 수 없습니다. 잠시 후 다시 시도해주세요."));
    }

    // 외부 API 키 미설정 → 503 Service Unavailable
    // 요청이 잘못된 게 아니라 서버가 아직 그 기능을 제공할 준비가 안 된 상태다.
    @ExceptionHandler(MissingApiKeyException.class)
    public ResponseEntity<ErrorResponse> handleMissingApiKey(MissingApiKeyException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(e.getMessage()));
    }

    // 쿼리 파라미터 타입 불일치 → 400 Bad Request
    // 예: /api/works?type=FOO  (WorkType enum에 없는 값)
    // 이걸 안 잡으면 스프링 기본 처리로 500이 나가 서버 오류처럼 보인다.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "요청 파라미터 '%s' 값이 올바르지 않습니다.".formatted(e.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }
}
