// 인증 상태를 앱 전체에 공급하는 Provider.
//
// 왜 Context가 필요한가?
//   localStorage는 '값'만 보관할 뿐, 값이 바뀌어도 React가 다시 그리지 않는다.
//   헤더가 로그인/로그아웃에 즉시 반응하려면 React 상태(state)로 들고 있어야 한다.
//   그 상태를 여러 화면(헤더·마이페이지·로그인)이 공유해야 하므로 Context를 쓴다.

import { useMemo, useState } from "react";
import type { ReactNode } from "react";
import { AuthContext } from "./AuthContext";
import type { AuthContextValue } from "./AuthContext";
import { getUser, saveAuth, clearAuth } from "../lib/auth";
import type { AuthUser } from "../lib/auth";

function AuthProvider({ children }: { children: ReactNode }) {
  // 초기값을 함수로 넘기면(lazy initializer) 첫 렌더 때 딱 한 번만 실행된다.
  // → 새로고침해도 localStorage에 남은 로그인 상태를 그대로 복원
  const [user, setUser] = useState<AuthUser | null>(() => getUser());

  // 로그인 성공 시: localStorage에 저장 + React 상태 갱신 → 헤더가 즉시 바뀜
  const login = (token: string, nextUser: AuthUser) => {
    saveAuth(token, nextUser);
    setUser(nextUser);
  };

  // 로그아웃: 저장된 토큰·회원정보 제거 + 상태 비움
  const logout = () => {
    clearAuth();
    setUser(null);
  };

  // useMemo로 감싸는 이유: value가 매 렌더마다 새 객체가 되면
  // 이 컨텍스트를 쓰는 모든 컴포넌트가 불필요하게 다시 그려진다.
  const value = useMemo<AuthContextValue>(
    () => ({ user, isLoggedIn: user !== null, login, logout }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthProvider;
