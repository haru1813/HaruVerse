// 인증 정보(JWT 토큰 + 로그인한 회원)를 localStorage에 보관하는 헬퍼.
// localStorage에 두는 이유: 새로고침·탭 재방문에도 로그인이 유지되게 하려고.
// (보안이 더 중요해지면 refresh token + httpOnly 쿠키 방식으로 바꾸는 게 정석)

const TOKEN_KEY = "haruverse_token";
const USER_KEY = "haruverse_user";

// 화면에서 쓰는 최소 회원 정보 — 백엔드 LoginResponse / MemberResponse와 대응
export type AuthUser = {
  email: string;
  nickname: string;
};

/* ── 토큰 ───────────────────────────────────────── */

export function saveToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

/* ── 회원 정보 ───────────────────────────────────── */

export function saveUser(user: AuthUser): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

// 저장된 회원 정보를 꺼냄. 값이 없거나 깨진 JSON이면 null.
// (localStorage는 사용자가 직접 수정할 수 있으므로 항상 방어적으로 파싱)
export function getUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as Partial<AuthUser>;
    if (typeof parsed?.email !== "string" || typeof parsed?.nickname !== "string") {
      return null;
    }
    return { email: parsed.email, nickname: parsed.nickname };
  } catch {
    return null;
  }
}

/* ── 공통 ───────────────────────────────────────── */

// 로그인 성공 시 토큰과 회원 정보를 한 번에 저장
export function saveAuth(token: string, user: AuthUser): void {
  saveToken(token);
  saveUser(user);
}

// 로그아웃 — 저장된 인증 정보를 모두 제거
export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

// 이전 이름 호환 (기존 코드에서 clearToken을 쓰고 있었다면 그대로 동작)
export const clearToken = clearAuth;

export function isLoggedIn(): boolean {
  return getToken() !== null;
}
