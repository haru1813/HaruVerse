package com.haru.haruverse.favorite.controller;

import com.haru.haruverse.favorite.service.FavoriteService;
import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.work.dto.WorkResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 찜 API.
 *
 * <p><b>왜 토글(POST /toggle)이 아니라 PUT/DELETE인가</b>
 * 토글은 같은 요청을 두 번 보내면 상태가 원래대로 돌아간다(멱등하지 않다).
 * 네트워크 재시도나 더블클릭이면 사용자 의도와 반대 결과가 나온다.
 * PUT은 "찜인 상태로 만들어라", DELETE는 "찜이 아닌 상태로 만들어라"라서
 * 몇 번을 보내든 결과가 같다.
 *
 * <p>인가는 SecurityConfig가 처리한다. {@code GET /api/works/**} 만 공개이므로
 * 아래 경로들은 자동으로 인증 필수다. (PUT·DELETE는 GET이 아니고, /api/favorites는 anyRequest에 걸림)
 *
 * <p>클래스 레벨 @RequestMapping을 두지 않은 이유: 찜은 작품에 매달린 하위 자원
 * ({@code /api/works/{id}/favorite})이면서, 내 목록은 회원 기준 자원
 * ({@code /api/favorites})이라 접두사가 둘로 갈린다.
 */
@RestController
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** 찜하기 — 새로 찜하면 201, 이미 찜한 상태면 200 */
    @PutMapping("/api/works/{workId}/favorite")
    public ResponseEntity<Void> add(@AuthenticationPrincipal String email,
                                    @PathVariable Long workId) {
        boolean created = favoriteService.add(email, workId);
        return created ? ResponseEntity.status(201).build() : ResponseEntity.ok().build();
    }

    /** 찜 해제 — 원래 찜이 아니었어도 204 (멱등) */
    @DeleteMapping("/api/works/{workId}/favorite")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal String email,
                                       @PathVariable Long workId) {
        favoriteService.remove(email, workId);
        return ResponseEntity.noContent().build();
    }

    /** 내가 찜한 작품 목록 (최신 찜 순) */
    @GetMapping("/api/favorites")
    public ResponseEntity<PageResponse<WorkResponse>> myFavorites(
            @AuthenticationPrincipal String email,
            // sort를 받지 않는다 — 정렬은 리포지토리 쿼리에 고정(찜한 시각 기준)
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(email, pageable));
    }

    /** 내가 찜한 작품 id 전체 — 목록 화면의 하트 상태 표시용 */
    @GetMapping("/api/favorites/ids")
    public ResponseEntity<List<Long>> myFavoriteIds(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(favoriteService.getMyFavoriteWorkIds(email));
    }
}
