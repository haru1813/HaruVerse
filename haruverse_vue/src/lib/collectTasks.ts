// 수집·색인 작업 정의.
//
// ★화면이 아니라 '데이터'로 둔다★
//   작업이 10개고 파라미터가 제각각이다. 각각을 컴포넌트로 만들면
//   비슷한 카드가 열 벌 생기고, 백엔드에 작업이 하나 늘 때마다 화면을 또 짠다.
//   여기 한 줄 추가하면 화면이 알아서 생기도록 정의만 둔다.

/** 입력 한 칸 */
export type Field =
  | { key: string; label: string; type: "number"; default: number; hint?: string }
  | { key: string; label: string; type: "text"; default: string; hint?: string }
  | { key: string; label: string; type: "switch"; default: boolean; hint?: string }
  | { key: string; label: string; type: "select"; default: string; options: string[]; hint?: string };

export type Task = {
  id: string;
  group: string;
  title: string;
  /** 이 작업이 무엇을 하는지 — 버튼을 누르기 전에 알아야 할 것만 */
  description: string;
  path: string;
  fields: Field[];
  /**
   * 오래 걸리는 작업인가.
   * 외부 API를 페이지 단위로 도는 것들이 여기 해당한다 — 몇 분씩 걸린다.
   */
  slow?: boolean;
  /** 되돌릴 수 없거나 파급이 큰 작업 — 실행 전에 한 번 더 묻는다 */
  confirm?: string;
};

const SEASONS = ["winter", "spring", "summer", "fall"];

/** 올해를 기본값으로 (연도를 매년 고치지 않도록) */
const THIS_YEAR = new Date().getFullYear();

export const TASKS: Task[] = [
  // ── 애니메이션 (Jikan / MyAnimeList) ──
  {
    id: "jikan-top",
    group: "애니메이션",
    title: "인기순 수집",
    description: "MyAnimeList 인기순으로 애니를 가져온다. 이미 있는 작품은 갱신된다.",
    path: "/api/collect/jikan/top",
    slow: true,
    fields: [
      { key: "pages", label: "페이지 수", type: "number", default: 1, hint: "1페이지 = limit개" },
      { key: "limit", label: "페이지당 개수", type: "number", default: 25 },
    ],
  },
  {
    id: "jikan-season",
    group: "애니메이션",
    title: "분기별 수집",
    description: "특정 연도·분기에 방영한 애니를 가져온다.",
    path: "/api/collect/jikan/season",
    slow: true,
    fields: [
      { key: "year", label: "연도", type: "number", default: THIS_YEAR },
      { key: "season", label: "분기", type: "select", default: "summer", options: SEASONS },
      { key: "pages", label: "페이지 수", type: "number", default: 1 },
      { key: "limit", label: "페이지당 개수", type: "number", default: 25 },
    ],
  },
  {
    id: "jikan-ids",
    group: "애니메이션",
    title: "MAL ID 지정 수집",
    description: "특정 작품만 골라 가져온다. 빠진 작품을 채울 때 쓴다.",
    path: "/api/collect/jikan/ids",
    fields: [
      { key: "ids", label: "MAL ID", type: "text", default: "", hint: "쉼표로 구분 (예: 52991, 1535)" },
    ],
  },
  {
    id: "jikan-characters",
    group: "애니메이션",
    title: "캐릭터 수집",
    description:
      "작품별 등장인물과 성우를 가져온다. ★Jikan이 504를 자주 반환해 지금은 대부분 실패한다★",
    path: "/api/collect/jikan/characters",
    slow: true,
    fields: [
      { key: "limit", label: "작품 수", type: "number", default: 20 },
      {
        key: "skipCollected",
        label: "이미 수집된 작품 건너뛰기",
        type: "switch",
        default: true,
        hint: "끄면 전부 다시 가져온다",
      },
    ],
  },

  // ── 게임 (RAWG) ──
  {
    id: "rawg-games",
    group: "게임",
    title: "게임 수집",
    description: "RAWG에서 게임을 가져온다. 기본은 메타크리틱 점수 높은 순.",
    path: "/api/collect/rawg/games",
    slow: true,
    fields: [
      { key: "pages", label: "페이지 수", type: "number", default: 1 },
      { key: "pageSize", label: "페이지당 개수", type: "number", default: 20 },
      {
        key: "ordering",
        label: "정렬",
        type: "select",
        default: "-metacritic",
        options: ["-metacritic", "-rating", "-released", "-added"],
      },
    ],
  },
  {
    id: "rawg-ids",
    group: "게임",
    title: "RAWG ID 지정 수집",
    description: "특정 게임만 골라 가져온다.",
    path: "/api/collect/rawg/ids",
    fields: [{ key: "ids", label: "RAWG ID", type: "text", default: "", hint: "쉼표로 구분" }],
  },

  // ── 한국어 · 기타 ──
  {
    id: "tmdb-titles",
    group: "한국어 제목",
    title: "TMDB 한국어 제목 수집",
    description:
      "영문 제목만 있는 작품에 한국어 제목을 채운다. 확신이 없으면 채우지 않는다 — 틀린 제목보다 빈 값이 낫다.",
    path: "/api/collect/tmdb/titles",
    slow: true,
    fields: [
      { key: "limit", label: "작품 수", type: "number", default: 20 },
      { key: "skipCollected", label: "이미 채워진 작품 건너뛰기", type: "switch", default: true },
    ],
  },
  {
    id: "starrail-characters",
    group: "한국어 제목",
    title: "스타레일 캐릭터 수집",
    description: "붕괴: 스타레일 캐릭터를 가져와 지정한 작품에 연결한다.",
    path: "/api/collect/starrail/characters",
    fields: [
      { key: "workId", label: "작품 ID", type: "number", default: 0, hint: "연결할 작품의 DB id" },
    ],
  },
  {
    id: "voice-actors-migrate",
    group: "한국어 제목",
    title: "성우 이관",
    description:
      "캐릭터에 문자열로 남아 있던 성우 이름을 성우 테이블로 옮기고 연결한다. 여러 번 돌려도 안전하다.",
    path: "/api/collect/voice-actors/migrate",
    fields: [],
  },

  // ── 검색 색인 ──
  {
    id: "reindex",
    group: "검색 색인",
    title: "Elasticsearch 재색인",
    description:
      "DB 전체를 읽어 검색 색인에 다시 넣는다. 색인이 DB와 어긋났을 때 쓴다.",
    path: "/api/search/reindex",
    slow: true,
    fields: [
      {
        key: "recreate",
        label: "색인을 지우고 다시 만들기",
        type: "switch",
        default: false,
        hint: "분석기 설정을 바꿨을 때만 켠다. 켜면 재색인이 끝날 때까지 검색이 비어 보인다",
      },
    ],
  },
];

/**
 * 결과 필드의 한국어 라벨.
 *
 * <p>응답 모양이 API마다 다르다(fetched/created 계열, works/matched 계열, scanned 계열…).
 * 화면은 응답의 키를 그대로 훑어 표시하고, 아는 키만 한국어로 바꾼다.
 * 백엔드가 필드를 늘려도 화면은 고치지 않아도 된다 — 원래 키가 그대로 보인다.
 */
export const RESULT_LABELS: Record<string, string> = {
  fetched: "가져옴",
  created: "신규",
  updated: "갱신",
  failed: "실패",
  failedPages: "실패한 페이지",
  works: "대상 작품",
  linked: "연결",
  skipped: "건너뜀",
  stopped: "중단됨",
  matched: "채움",
  unmatched: "후보 없음",
  scanned: "검사",
  indexed: "색인",
  recreated: "재생성",
  ok: "성공",
  message: "메시지",
};

/** 값이 0보다 크면 눈에 띄어야 하는 키 (실패 계열) */
export const WARN_KEYS = new Set(["failed", "failedPages", "stopped", "unmatched"]);

/** 성과를 나타내는 키 — 0보다 크면 초록 */
export const GOOD_KEYS = new Set(["created", "updated", "matched", "linked", "indexed", "fetched"]);
