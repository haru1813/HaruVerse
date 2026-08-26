// API 호출 헬퍼 — 보호된 엔드포인트를 부를 때마다 Authorization 헤더를 손으로
// 붙이면 빠뜨리기 쉬우니, 여기서 한 번에 처리한다.
//
// 요청 경로는 항상 '/api/...' 상대경로를 쓴다.
// → 브라우저는 같은 출처(:5173)로 보내고, Vite 프록시가 백엔드(:8080)로 전달 (CORS 없음)

import { getToken, clearAuth } from "./auth";

// 401(인증 만료·위조 토큰)일 때 던지는 전용 에러.
// 화면 쪽에서 "instanceof UnauthorizedError" 로 구분해 로그인 화면으로 보낼 수 있다.
export class UnauthorizedError extends Error {
  constructor(message = "인증이 필요합니다. 다시 로그인해주세요.") {
    super(message);
    this.name = "UnauthorizedError";
  }
}

// 인증이 필요한 요청용 fetch.
// - 토큰이 있으면 Authorization 헤더를 자동으로 붙임
// - 401이면 저장된 인증 정보를 지우고 UnauthorizedError를 던짐
//   (토큰 만료 1시간 → 만료된 토큰을 계속 들고 있지 않도록)
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();

  const res = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });

  if (res.status === 401) {
    clearAuth();
    throw new UnauthorizedError();
  }

  if (!res.ok) {
    // 백엔드 GlobalExceptionHandler는 { "message": "..." } 형태로 내려준다
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? `요청에 실패했습니다. (HTTP ${res.status})`);
  }

  // ★바디가 없는 응답을 그냥 json()으로 파싱하면 터진다★
  //   찜 API는 204 No Content(해제)나 빈 바디의 201(생성)을 돌려준다.
  //   text로 먼저 읽고, 비어 있으면 undefined를 반환한다.
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
