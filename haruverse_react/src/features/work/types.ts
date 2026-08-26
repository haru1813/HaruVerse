// 백엔드 work 도메인과 1:1로 맞춘 타입.
// 서버 DTO(WorkResponse·WorkDetailResponse·PageResponse)가 바뀌면 여기도 같이 고친다.

export type WorkType = "ANIME" | "GAME";
export type WorkSource = "JIKAN" | "RAWG" | "MANUAL";

/** 목록용 — 백엔드 WorkResponse */
export type Work = {
  id: number;
  title: string;
  type: WorkType;
  season: string | null;      // "2023-fall"
  rating: number | null;      // 9.3
  imageUrl: string | null;
  releaseDate: string | null; // "2023-09-29"
  genres: string[];           // ["Action", "Fantasy"]
  platforms: string[];        // ["PC", "PlayStation"] — 게임만. 애니는 빈 배열
};

/** 상세용 — 백엔드 WorkDetailResponse */
export type WorkDetail = Work & {
  synopsis: string | null;
  source: WorkSource;
  externalId: string | null;
  studio: string | null;      // 제작사 이름
  createdAt: string;
  updatedAt: string;
};

/** 페이징 응답 — 백엔드 PageResponse */
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

/** "2023-fall" → "2023 가을" (화면 표시용) */
const SEASON_LABEL: Record<string, string> = {
  spring: "봄",
  summer: "여름",
  fall: "가을",
  winter: "겨울",
};

export function formatSeason(season: string | null): string | undefined {
  if (!season) return undefined;
  const [year, name] = season.split("-");
  const label = SEASON_LABEL[name];
  return label ? `${year} ${label}` : season;
}

/**
 * 카드 하단에 쓸 부제.
 *
 * 애니는 분기("2023 가을")가 있지만 게임은 분기 개념이 없다.
 * → 게임은 출시연도("2013년")로 대체해 카드 높이가 들쭉날쭉하지 않게 한다.
 */
/**
 * 플랫폼 표시 순서.
 *
 * <p>★RAWG는 알파벳순으로 준다★ — 그대로 두면 발더스 게이트 3이
 * "Apple Macintosh, PC"가 된다. 카드는 두 개만 보여주므로,
 * 가장 대표성 없는 둘이 뽑히는 셈이다.
 * → 주요 콘솔·PC를 앞으로 끌어올린다. 목록에 없는 플랫폼은 뒤에 알파벳순으로 붙는다.
 */
const PLATFORM_ORDER = [
  "PC", "PlayStation", "Xbox", "Nintendo", "SEGA",
  "iOS", "Android", "Apple Macintosh", "Linux",
];

export function sortPlatforms(platforms: string[]): string[] {
  const rank = (p: string) => {
    const i = PLATFORM_ORDER.indexOf(p);
    return i === -1 ? PLATFORM_ORDER.length : i;
  };
  return [...platforms].sort((a, b) => rank(a) - rank(b) || a.localeCompare(b));
}

export function workSubtitle(
  work: Pick<Work, "season" | "releaseDate" | "platforms">,
): string | undefined {
  const season = formatSeason(work.season);
  if (season) return season; // 애니 — 분기가 있으면 그대로

  const year = work.releaseDate ? `${work.releaseDate.slice(0, 4)}년` : undefined;

  // 게임은 분기가 없으니 "출시연도 · 플랫폼"으로 채운다.
  // ★2개까지만★ — 부제는 noWrap 한 줄이라 더 넣으면 잘려서 오히려 안 보인다.
  //   (전부 보려면 상세 페이지로)
  const platforms = sortPlatforms(work.platforms ?? []).slice(0, 2).join(", ");
  if (year && platforms) return `${year} · ${platforms}`;
  return year ?? platforms ?? undefined;
}
