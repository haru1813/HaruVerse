// 채널 구독 API — /api/works/{id}/subscription, /api/subscriptions
//
// ★내 구독 목록이 /api/community/ 가 아니라 /api/subscriptions 인 이유★
// 백엔드 SecurityConfig에서 GET /api/community/** 는 permitAll 이다(비로그인도 커뮤니티를
// 읽을 수 있어야 하므로 맞는 설정). 거기에 뒀다면 비로그인 요청이 인증 없이 통과해버린다.
// /api/subscriptions 는 anyRequest().authenticated() 에 걸려 401로 막힌다.

import { apiFetch } from "../../lib/api";
import type { Channel } from "./api";

/**
 * 구독하기.
 *
 * PUT을 쓰는 이유 — "구독 상태로 만들어라"라는 뜻이라 몇 번을 보내도 결과가 같다.
 * 토글(POST)이었다면 더블클릭이나 재시도에서 상태가 뒤집힌다. (찜과 같은 이유)
 */
export function subscribe(workId: number): Promise<void> {
  return apiFetch<void>(`/api/works/${workId}/subscription`, { method: "PUT" });
}

/** 구독 해제 — 원래 구독이 아니었어도 성공(204) */
export function unsubscribe(workId: number): Promise<void> {
  return apiFetch<void>(`/api/works/${workId}/subscription`, { method: "DELETE" });
}

/** 내가 구독한 작품 id 전체 — 버튼 상태를 칠하기 위해 한 번에 받는다 */
export function fetchSubscriptionIds(): Promise<number[]> {
  return apiFetch<number[]>("/api/subscriptions/ids");
}

/**
 * 내 구독 채널 목록 — 커뮤니티 첫 화면 상단.
 *
 * 페이징이 없다(배열이 그대로 온다). 구독은 본인이 하나씩 눌러 만든 목록이라 수가
 * 제한적이고, 첫 화면 상단 섹션에 페이지네이션 UI는 오히려 방해가 된다.
 *
 * ★글이 0개인 채널도 들어 있다★ — latestPost* 가 null 인 카드를 그릴 수 있어야 한다.
 */
export function fetchMySubscriptions(): Promise<Channel[]> {
  return apiFetch<Channel[]>("/api/subscriptions");
}
