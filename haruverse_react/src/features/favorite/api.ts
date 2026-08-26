// 찜 API — /api/works/{id}/favorite, /api/favorites
//
// 전부 인증이 필요하다(백엔드 SecurityConfig에서 GET /api/works/** 만 공개).
// 토큰이 없거나 만료면 apiFetch가 UnauthorizedError를 던진다.

import { apiFetch } from "../../lib/api";
import type { PageResponse, Work } from "../work/types";

/**
 * 찜하기.
 *
 * PUT을 쓰는 이유 — "찜인 상태로 만들어라"라는 뜻이라 몇 번을 보내도 결과가 같다.
 * 토글(POST)이었다면 더블클릭이나 재시도에서 상태가 뒤집힌다.
 */
export function addFavorite(workId: number): Promise<void> {
  return apiFetch<void>(`/api/works/${workId}/favorite`, { method: "PUT" });
}

/** 찜 해제 — 원래 찜이 아니었어도 성공(204) */
export function removeFavorite(workId: number): Promise<void> {
  return apiFetch<void>(`/api/works/${workId}/favorite`, { method: "DELETE" });
}

/** 내가 찜한 작품 id 전체 — 목록의 하트를 칠하기 위해 한 번에 받는다 */
export function fetchFavoriteIds(): Promise<number[]> {
  return apiFetch<number[]>("/api/favorites/ids");
}

/** 내가 찜한 작품 목록(최신 찜 순) — 마이페이지에서 사용 */
export function fetchMyFavorites(page = 0, size = 24): Promise<PageResponse<Work>> {
  return apiFetch<PageResponse<Work>>(`/api/favorites?page=${page}&size=${size}`);
}
