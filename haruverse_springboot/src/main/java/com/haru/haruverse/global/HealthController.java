package com.haru.haruverse.global;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 헬스 체크 — 서버가 살아있는지 확인하는 간단한 엔드포인트.
// (Playwright 테스트가 '백엔드 준비 완료'를 감지하는 데 사용)
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "ok";
    }
}
