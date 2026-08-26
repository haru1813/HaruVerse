// 제작사 API — /api/studios
//
// 비로그인도 볼 수 있다 (백엔드 SecurityConfig에서 GET permitAll).

import { apiFetch } from "../../lib/api";
import type { PageResponse } from "../work/types";

/** 백엔드 StudioResponse — 목록에는 작품 수가 함께 온다 */
export type Studio = {
  id: number;
  name: string;
  workCount: number;
};

export type StudioQuery = {
  keyword?: string;
  page?: number; // 0부터
  size?: number;
};

/**
 * 제작사 목록 — 작품이 많은 순.
 *
 * 정렬 파라미터를 보내지 않는다. 백엔드 쿼리에 order by가 고정돼 있다.
 */
export function fetchStudios(query: StudioQuery = {}): Promise<PageResponse<Studio>> {
  const params = new URLSearchParams();
  if (query.keyword?.trim()) params.set("q", query.keyword.trim());
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 24));

  return apiFetch<PageResponse<Studio>>(`/api/studios?${params.toString()}`);
}
