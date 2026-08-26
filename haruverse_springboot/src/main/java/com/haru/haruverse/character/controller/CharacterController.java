package com.haru.haruverse.character.controller;

import com.haru.haruverse.character.dto.CharacterDetailResponse;
import com.haru.haruverse.character.dto.CharacterResponse;
import com.haru.haruverse.character.dto.WorkCharacterResponse;
import com.haru.haruverse.character.service.CharacterService;
import com.haru.haruverse.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 캐릭터 API — 전부 공개(비로그인 열람 가능).
 *
 * <p>SecurityConfig에 {@code GET /api/characters/**} 를 permitAll로 열어줘야 한다.
 * (작품 API와 같은 취급 — 도감은 로그인 없이 볼 수 있어야 한다)
 */
@RestController
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    /** 캐릭터 목록 — GET /api/characters?q=frieren&page=0&size=24 */
    @GetMapping("/api/characters")
    public ResponseEntity<PageResponse<CharacterResponse>> getCharacters(
            @RequestParam(name = "q", required = false) String keyword,
            // 정렬은 인기순으로 고정 — 쿼리에 order by가 박혀 있어 sort를 받지 않는다
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(characterService.getCharacters(keyword, pageable));
    }

    /** 캐릭터 상세 — GET /api/characters/{id} */
    @GetMapping("/api/characters/{id}")
    public ResponseEntity<CharacterDetailResponse> getCharacter(@PathVariable Long id) {
        return ResponseEntity.ok(characterService.getCharacter(id));
    }

    /** 작품의 등장인물 — GET /api/works/{workId}/characters */
    @GetMapping("/api/works/{workId}/characters")
    public ResponseEntity<List<WorkCharacterResponse>> getCharactersOfWork(@PathVariable Long workId) {
        return ResponseEntity.ok(characterService.getCharactersOfWork(workId));
    }
}
