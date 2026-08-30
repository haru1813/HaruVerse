// 백엔드 호출 래퍼 — 토큰 첨부와 오류 처리를 한곳에 모은다.
//
// ★같은 오리진으로 부른다★
//   경로를 "/api/..." 로만 쓴다. 절대 URL(https://haruverse.haru.company/api/...)을
//   쓰면 관리자 콘솔이 다른 서브도메인이므로 크로스 오리진이 되고,
//   백엔드에 없는 CORS 설정을 새로 만들어야 한다.
//   개발에서는 vite 프록시가, 운영에서는 컨테이너의 nginx가 이 경로를 백엔드로 넘긴다.
//   덕분에 브라우저 입장에서는 언제나 same-origin이다.

import { clearToken, getToken } from "./auth";

/** 인증 만료를 알리는 전역 이벤트 이름 */
export const UNAUTHORIZED_EVENT = "haruverse:unauthorized";

export class ApiError extends Error {
  // 생성자 파라미터 프로퍼티(constructor(public status: ...))를 쓰지 않는다 —
  // 이 프로젝트는 erasableSyntaxOnly 를 켜 두어, 타입만 지우면 그대로 JS가 되는
  // 문법만 허용한다. 파라미터 프로퍼티는 코드를 생성하므로 그 규칙에 어긋난다.
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

type Options = {
  method?: string;
  /** JSON 본문 (있으면 Content-Type을 자동으로 붙인다) */
  body?: unknown;
  /** 쿼리 파라미터 — undefined/null 인 값은 빠진다 */
  params?: Record<string, string | number | boolean | undefined | null>;
  /**
   * 인증 없이 부른다 (로그인 API 전용).
   * 만료된 토큰이 남아 있을 때 로그인 요청까지 401로 튕기는 걸 막는다.
   */
  anonymous?: boolean;
  signal?: AbortSignal;
};

export async function api<T>(path: string, options: Options = {}): Promise<T> {
  const { method = "GET", body, params, anonymous = false, signal } = options;

  const headers: Record<string, string> = {};

  if (!anonymous) {
    const token = getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined) headers["Content-Type"] = "application/json";

  const response = await fetch(path + buildQuery(params), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });

  // ★401은 화면 전체의 문제다★
  //   토큰이 만료됐거나 유효하지 않다. 개별 호출부가 저마다 처리하면
  //   화면마다 다르게 반응한다. 여기서 토큰을 버리고 한 번만 알린다.
  //   라우터를 직접 부르지 않는 이유는 순환 참조 때문이다
  //   (router → views → api → router). 이벤트로 끊는다.
  if (response.status === 401 && !anonymous) {
    clearToken();
    window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
    throw new ApiError(401, "로그인이 만료되었습니다. 다시 로그인해 주세요.");
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response));
  }

  // 204 No Content — 삭제 API가 본문 없이 온다. json()을 부르면 예외가 난다.
  if (response.status === 204 || response.headers.get("content-length") === "0") {
    return undefined as T;
  }

  return (await response.json()) as T;
}

function buildQuery(params: Options["params"]): string {
  if (!params) return "";

  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === "") continue;
    search.append(key, String(value));
  }

  const query = search.toString();
  return query ? `?${query}` : "";
}

/**
 * 오류 본문에서 사람이 읽을 문장을 뽑는다.
 *
 * <p>백엔드가 상황에 따라 세 가지 모양으로 답한다.
 * 스프링 기본 오류({@code {"message": ...}}), 우리가 던진 문자열, 그리고 빈 본문.
 * 무엇이 오든 화면에는 문장 하나가 떠야 한다.
 */
async function readErrorMessage(response: Response): Promise<string> {
  const fallback = `요청이 실패했습니다 (HTTP ${response.status})`;

  try {
    const text = await response.text();
    if (!text) return fallback;

    try {
      const json = JSON.parse(text);
      return json.message || json.error || fallback;
    } catch {
      // JSON이 아니면 본문 자체가 메시지다. 너무 길면 화면이 무너지므로 자른다.
      return text.length > 200 ? fallback : text;
    }
  } catch {
    return fallback;
  }
}
