// 캐릭터 API — /api/characters, /api/works/{id}/characters
//
// 전부 비로그인도 볼 수 있다 (백엔드 SecurityConfig에서 GET permitAll).

import { apiFetch } from "../../lib/api";
import type { PageResponse } from "../work/types";
import type { CharacterDetail, CharacterSummary, WorkCharacter } from "./types";

export type CharacterQuery = {
  keyword?: string;
  page?: number; // 0부터
  size?: number;
};

/**
 * 캐릭터 목록 — 인기순 고정.
 *
 * 정렬 파라미터를 보내지 않는다. 백엔드 쿼리에 order by가 박혀 있어
 * sort를 함께 넘기면 충돌한다.
 */
export function fetchCharacters(query: CharacterQuery = {}): Promise<PageResponse<CharacterSummary>> {
  const params = new URLSearchParams();
  if (query.keyword?.trim()) params.set("q", query.keyword.trim());
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 24));

  return apiFetch<PageResponse<CharacterSummary>>(`/api/characters?${params.toString()}`);
}

/** 캐릭터 상세 (출연 작품 포함) */
export function fetchCharacter(id: number): Promise<CharacterDetail> {
  return apiFetch<CharacterDetail>(`/api/characters/${id}`);
}

/** 작품의 등장인물 — 주역 먼저, 그 안에서 인기순 */
export function fetchWorkCharacters(workId: number): Promise<WorkCharacter[]> {
  return apiFetch<WorkCharacter[]>(`/api/works/${workId}/characters`);
}
