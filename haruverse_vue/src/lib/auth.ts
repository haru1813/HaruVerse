// 관리자 콘솔의 인증 상태 — JWT 보관과 권한 판별.
//
// ★서비스 앱(haruverse_react)과 로그인이 공유되지 않는다★
//   localStorage는 오리진마다 별개다. haruverse.haru.company에서 로그인해 두어도
//   admin-haruverse.haru.company는 그 토큰을 볼 수 없다.
//   불편이 아니라 격리다 — 서비스 세션이 관리자 권한으로 새지 않는다.
//   키 이름도 일부러 다르게 두어, 같은 오리진에 올리더라도 섞이지 않게 한다.

const TOKEN_KEY = "haruverse.admin.token";

/** 로그인 응답 — 백엔드 LoginResponse 그대로 (role은 없다, 아래 참고) */
export type LoginResponse = {
  token: string;
  email: string;
  nickname: string;
};

/** 토큰에서 읽어낸 신원 */
export type Identity = {
  email: string;
  role: string;
  /** 만료 시각 (epoch ms) */
  expiresAt: number;
};

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

/**
 * JWT 페이로드를 풀어 신원을 읽는다.
 *
 * <p>★서명은 검증하지 않는다★ 검증은 백엔드가 한다.
 * 여기서 읽는 값은 <b>화면을 어떻게 그릴지</b>에만 쓴다 —
 * 메뉴를 보일지, 로그인 화면으로 돌려보낼지.
 * 사용자가 페이로드를 조작해 role을 ADMIN으로 바꿔도
 * 실제 API는 서명을 검증하므로 403이 돌아온다.
 * 즉 이 함수는 <b>편의</b>이지 <b>방어</b>가 아니다.
 *
 * <p>백엔드 LoginResponse에 role이 없어서 토큰에서 꺼낸다.
 * (JwtTokenProvider가 subject=이메일, "role" 클레임에 권한을 넣는다)
 */
export function readIdentity(token: string | null = getToken()): Identity | null {
  if (!token) return null;

  const parts = token.split(".");
  if (parts.length !== 3) return null;

  try {
    const payload = JSON.parse(decodeSegment(parts[1]));
    if (!payload.sub || !payload.role || !payload.exp) return null;

    return {
      email: String(payload.sub),
      role: String(payload.role),
      // JWT의 exp는 '초' 단위다. Date와 비교하려면 ms로 올려야 한다 —
      // 안 그러면 1970년으로 읽혀 모든 토큰이 만료된 것으로 판정된다.
      expiresAt: Number(payload.exp) * 1000,
    };
  } catch {
    // 토큰이 깨졌거나 우리 형식이 아니다 → 로그인하지 않은 것으로 본다
    return null;
  }
}

/**
 * base64url 세그먼트를 UTF-8 문자열로 되돌린다.
 *
 * <p>★atob만으로는 안 된다★ 두 가지 이유가 있다.
 * <ol>
 *   <li>JWT는 base64가 아니라 <b>base64url</b>이다 — {@code +/} 대신 {@code -_} 를 쓰고
 *       끝의 {@code =} 패딩이 없다. 그대로 넣으면 디코드가 실패한다</li>
 *   <li>atob는 <b>바이트</b>를 준다. 닉네임에 한글이 들어가면 UTF-8 다중 바이트가
 *       글자당 하나씩 깨져 나온다</li>
 * </ol>
 */
function decodeSegment(segment: string): string {
  const base64 = segment.replace(/-/g, "+").replace(/_/g, "/");
  // 길이를 4의 배수로 맞춘다
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");

  const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
  return new TextDecoder("utf-8").decode(bytes);
}

/** 토큰이 있고, 아직 만료되지 않았는가 */
export function isLoggedIn(): boolean {
  const id = readIdentity();
  return id !== null && id.expiresAt > Date.now();
}

/**
 * 관리자인가.
 *
 * <p>백엔드 MemberRole은 {@code USER} / {@code ADMIN} 이고,
 * 토큰에는 {@code role.name()} 이 그대로 담긴다("ROLE_" 접두어는 없다).
 */
export function isAdmin(): boolean {
  return isLoggedIn() && readIdentity()?.role === "ADMIN";
}
