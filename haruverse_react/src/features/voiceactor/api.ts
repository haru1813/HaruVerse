// 성우 API — /api/voice-actors
//
// 비로그인도 볼 수 있다 (백엔드 SecurityConfig에서 GET permitAll).

import { apiFetch } from "../../lib/api";
import type { PageResponse } from "../work/types";
import type { CharacterSummary } from "../character/types";

/** 목록용 — 백엔드 VoiceActorResponse */
export type VoiceActorSummary = {
  id: number;
  /**
   * MyAnimeList 인물 번호.
   * 이름만 이관된 성우는 null이다 (재수집 때 채워진다).
   */
  malId: number | null;
  name: string;
  imageUrl: string | null;
  characterCount: number;
};

/** 상세 — 맡은 캐릭터 목록이 함께 온다 */
export type VoiceActorDetail = {
  id: number;
  malId: number | null;
  name: string;
  imageUrl: string | null;
  characters: CharacterSummary[];
};

export type VoiceActorQuery = {
  keyword?: string;
  page?: number; // 0부터
  size?: number;
};

/** 성우 목록 — 맡은 캐릭터가 많은 순 (정렬은 백엔드 고정) */
export function fetchVoiceActors(query: VoiceActorQuery = {}): Promise<PageResponse<VoiceActorSummary>> {
  const params = new URLSearchParams();
  if (query.keyword?.trim()) params.set("q", query.keyword.trim());
  params.set("page", String(query.page ?? 0));
  params.set("size", String(query.size ?? 24));

  return apiFetch<PageResponse<VoiceActorSummary>>(`/api/voice-actors?${params.toString()}`);
}

/** 성우 상세 (맡은 캐릭터 포함) */
export function fetchVoiceActor(id: number): Promise<VoiceActorDetail> {
  return apiFetch<VoiceActorDetail>(`/api/voice-actors/${id}`);
}
