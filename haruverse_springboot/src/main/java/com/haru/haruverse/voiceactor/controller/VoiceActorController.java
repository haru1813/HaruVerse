package com.haru.haruverse.voiceactor.controller;

import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.voiceactor.dto.VoiceActorDetailResponse;
import com.haru.haruverse.voiceactor.dto.VoiceActorResponse;
import com.haru.haruverse.voiceactor.service.VoiceActorService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 성우 API — 비로그인도 볼 수 있다 (작품·캐릭터·제작사와 같은 취급).
 *
 * <p>경로에 하이픈을 쓴 이유: {@code /api/voiceactors} 보다 {@code /api/voice-actors} 가
 * URL 관례에 맞고 읽기도 낫다.
 */
@RestController
@RequestMapping("/api/voice-actors")
public class VoiceActorController {

    private final VoiceActorService voiceActorService;

    public VoiceActorController(VoiceActorService voiceActorService) {
        this.voiceActorService = voiceActorService;
    }

    /** 성우 목록 — GET /api/voice-actors?q=tanezaki&page=0&size=24 (담당 캐릭터 많은 순) */
    @GetMapping
    public ResponseEntity<PageResponse<VoiceActorResponse>> getVoiceActors(
            @RequestParam(required = false, name = "q") String keyword,
            // 정렬은 쿼리에 고정이라 sort를 받지 않는다
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(voiceActorService.getVoiceActors(keyword, pageable));
    }

    /** 성우 상세 — GET /api/voice-actors/{id} (맡은 캐릭터 포함) */
    @GetMapping("/{id}")
    public ResponseEntity<VoiceActorDetailResponse> getVoiceActor(@PathVariable Long id) {
        return ResponseEntity.ok(voiceActorService.getVoiceActor(id));
    }
}
