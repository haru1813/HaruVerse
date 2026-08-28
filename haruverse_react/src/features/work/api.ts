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

/** 자동완성 후보 — 검색창에 뜨는 최소 정보 */
export type Suggestion = {
  id: number;
  title: string;
  type: WorkType;
  imageUrl: string | null;
};

/**
 * 자동완성 — GET /api/works/suggest
 *
 * ★실패해도 예외를 던지지 않는다★
 * 검색창은 타이핑마다 이걸 부른다. 여기서 예외가 나면 입력 중에 화면이 깨진다.
 * 자동완성은 없으면 그냥 안 뜨면 되는 기능이라 빈 배열로 조용히 넘어간다.
 */
export async function fetchSuggestions(keyword: string, size = 8): Promise<Suggestion[]> {
  if (!keyword.trim()) return [];
  try {
    return await apiFetch<Suggestion[]>(
      `/api/works/suggest?q=${encodeURIComponent(keyword.trim())}&size=${size}`,
    );
  } catch {
    return [];
  }
}
