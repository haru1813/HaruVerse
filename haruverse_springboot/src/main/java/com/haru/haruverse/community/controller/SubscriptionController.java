package com.haru.haruverse.community.controller;

import com.haru.haruverse.community.dto.ChannelResponse;
import com.haru.haruverse.community.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채널 구독 API.
 *
 * <p><b>왜 토글이 아니라 PUT/DELETE인가</b> — 찜과 같다. 토글은 두 번 보내면 상태가
 * 원래대로 돌아가서(멱등하지 않다) 재시도·더블클릭이 사용자 의도와 반대 결과를 낳는다.
 * PUT은 "구독 상태로 만들어라", DELETE는 "구독이 아닌 상태로 만들어라"라서
 * 몇 번을 보내든 결과가 같다.
 *
 * <p><b>★내 구독 목록을 /api/community/ 아래 두지 않은 이유★</b>
 * SecurityConfig에 {@code GET /api/community/**} 가 permitAll로 열려 있다(커뮤니티는
 * 비로그인도 읽을 수 있어야 하므로 맞는 설정이다). 거기에 내 구독 목록을 두면
 * <b>비로그인 요청이 인증 없이 통과해</b> principal이 null인 채 서비스까지 도달한다.
 * {@code /api/subscriptions} 는 anyRequest().authenticated() 에 걸려 401로 막힌다.
 * (찜의 /api/favorites 와 같은 배치)
 */
@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** 구독 — 새로 구독하면 201, 이미 구독 중이면 200 */
    @PutMapping("/api/works/{workId}/subscription")
    public ResponseEntity<Void> subscribe(@AuthenticationPrincipal String email,
                                          @PathVariable Long workId) {
        boolean created = subscriptionService.subscribe(email, workId);
        return created ? ResponseEntity.status(201).build() : ResponseEntity.ok().build();
    }

    /** 구독 해제 — 원래 구독이 아니었어도 204 (멱등) */
    @DeleteMapping("/api/works/{workId}/subscription")
    public ResponseEntity<Void> unsubscribe(@AuthenticationPrincipal String email,
                                            @PathVariable Long workId) {
        subscriptionService.unsubscribe(email, workId);
        return ResponseEntity.noContent().build();
    }

    /** 내 구독 채널 목록 — 최근 글이 있는 채널부터 */
    @GetMapping("/api/subscriptions")
    public ResponseEntity<List<ChannelResponse>> myChannels(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(subscriptionService.getMyChannels(email));
    }

    /** 내가 구독한 작품 id 전체 — 구독 버튼 상태 표시용 */
    @GetMapping("/api/subscriptions/ids")
    public ResponseEntity<List<Long>> myIds(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(subscriptionService.getMyWorkIds(email));
    }
}
