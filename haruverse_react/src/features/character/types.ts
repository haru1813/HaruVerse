// 백엔드 character 도메인과 1:1로 맞춘 타입.

export type CharacterRole = "MAIN" | "SUPPORTING";

/** 목록용 — 백엔드 CharacterResponse */
export type CharacterSummary = {
  id: number;
  externalId: string;   // "jikan-188175" / "hsr-1001" — 출처가 접두사로 구분된다
  name: string;
  imageUrl: string | null;
  favorites: number;      // MAL 즐겨찾기 수
  voiceActor: string | null;   // 일본어 성우 이름 (없을 수 있음)
  voiceActorId: number | null; // 성우 상세로 가기 위한 id (성우 정보가 없으면 null)
};

/** 작품 상세의 등장인물 — 백엔드 WorkCharacterResponse (역할이 더 붙는다) */
export type WorkCharacter = CharacterSummary & { role: CharacterRole };

/** 캐릭터 상세 — 백엔드 CharacterDetailResponse */
export type CharacterDetail = CharacterSummary & {
  appearances: {
    workId: number;
    title: string;
    imageUrl: string | null;
    role: CharacterRole;
  }[];
};

export const ROLE_LABEL: Record<CharacterRole, string> = {
  MAIN: "주역",
  SUPPORTING: "조역",
};

/**
 * 즐겨찾기 수를 짧게 — 32400 → "32.4K".
 *
 * 카드가 좁아서 다섯 자리 숫자를 그대로 두면 줄이 넘어간다.
 */
export function formatFavorites(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}
