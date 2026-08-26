// 인증 컨텍스트 '정의' 파일 — Context 객체와 useAuth 훅만 둔다.
// Provider 컴포넌트(JSX)는 AuthProvider.tsx로 분리했다.
//   → 한 파일에서 컴포넌트와 비컴포넌트를 함께 export하면
//     Vite의 Fast Refresh(수정 시 상태 유지 갱신)가 깨지기 때문.

import { createContext, useContext } from "react";
import type { AuthUser } from "../lib/auth";

export type AuthContextValue = {
  user: AuthUser | null; // 로그인한 회원 (비로그인이면 null)
  isLoggedIn: boolean; // user !== null 과 같지만, 화면에서 읽기 쉬우라고 별도 제공
  login: (token: string, user: AuthUser) => void; // 로그인 성공 시 호출
  logout: () => void; // 로그아웃 시 호출
};

// 기본값 undefined → Provider 밖에서 쓰면 useAuth가 에러를 던져 실수를 빨리 잡는다
export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

// 화면에서 인증 상태를 꺼내 쓰는 훅
//   const { user, isLoggedIn, logout } = useAuth();
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth는 <AuthProvider> 안에서만 사용할 수 있습니다.");
  }
  return ctx;
}
