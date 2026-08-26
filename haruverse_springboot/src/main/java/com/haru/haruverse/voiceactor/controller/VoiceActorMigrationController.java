package com.haru.haruverse.voiceactor.controller;

import com.haru.haruverse.voiceactor.service.VoiceActorMigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 성우 이관 실행기 — 일회성 관리 작업.
 *
 * <p>{@code /api/collect/**} 아래에 둬서 다른 수집 API와 같이 <b>인증이 필요하다</b>.
 * (SecurityConfig에서 GET만 열려 있고 POST는 anyRequest().authenticated()에 걸린다)
 *
 * <p>여러 번 호출해도 안전하다 — 이미 성우가 연결된 캐릭터는 대상에서 빠진다.
 */
@RestController
@RequestMapping("/api/collect/voice-actors")
public class VoiceActorMigrationController {

    private final VoiceActorMigrationService migrationService;

    public VoiceActorMigrationController(VoiceActorMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /** POST /api/collect/voice-actors/migrate — 예전 문자열 컬럼을 성우 엔티티로 옮긴다 */
    @PostMapping("/migrate")
    public ResponseEntity<VoiceActorMigrationService.MigrationResult> migrate() {
        return ResponseEntity.ok(migrationService.migrate());
    }
}
