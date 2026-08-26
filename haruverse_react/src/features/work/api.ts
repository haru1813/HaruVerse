// 작품 API 호출 — /api/works
//
// 이 API는 비로그인도 볼 수 있지만(백엔드에서 permitAll),
// apiFetch를 쓰면 토큰이 있을 때 자동으로 붙고 401 처리도 공통으로 된다.

import { apiFetch } from "../../lib/api";
import type { PageResponse, Work, WorkDetail, WorkType } from "./types";

export type WorkQuery = {
  type?: WorkType;
  season?: string;
  genre?: string;
  studio?: string;
  keyword?: string;
  page?: number; // 0부터
  size?: number;
};

/**
 * 작품 목록.
 *
 * <p>URLSearchParams를 쓰는 이유: 값에 공백·한글·＆ 등이 들어가도
 * 알아서 인코딩해준다. 문자열을 직접 이어붙이면 검색어에 &가 하나만 있어도 깨진다.
 */
export function fetchWorks(query: WorkQuery = {}): Promise<PageResponse<Work>> {
  const params = new URLSearchParams();

  if (query.type) params.set("type", query.type);
  if (query.season) params.set("season", query.season);
  if (query.genre) params.set("genre", query.genre);
  if (query.studio) params.set("studio", query.studio);
  if (query.keyword?.trim()) params.set("q", query.keyword.trim());
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 20));

  return apiFetch<PageResponse<Work>>(`/api/works?${params.toString()}`);
}

/** 작품 상세 — 없으면 404 → apiFetch가 Error를 던짐 */
export function fetchWork(id: number): Promise<WorkDetail> {
  return apiFetch<WorkDetail>(`/api/works/${id}`);
}
