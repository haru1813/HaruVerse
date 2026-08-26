// 찜 컨텍스트 '정의' 파일 — Context 객체와 훅만 둔다.
// (Provider는 FavoriteProvider.tsx로 분리 — AuthContext와 같은 이유로 Fast Refresh 보호)

import { createContext, useContext } from "react";

export type FavoriteContextValue = {
  /** 내가 찜한 작품 id 집합 — 배열이 아니라 Set인 이유는 카드마다 O(1)로 조회하기 위해 */
  favoriteIds: Set<number>;
  isFavorite: (workId: number) => boolean;
  /** 찜/해제를 뒤집는다. 화면은 즉시 반영되고, 실패하면 자동으로 되돌아간다 */
  toggleFavorite: (workId: number) => Promise<void>;
  /** 최초 로딩 중인지 (하트를 깜빡이지 않게 하려고 노출) */
  loading: boolean;
};

export const FavoriteContext = createContext<FavoriteContextValue | undefined>(undefined);

export function useFavorite(): FavoriteContextValue {
  const ctx = useContext(FavoriteContext);
  if (!ctx) {
    throw new Error("useFavorite는 <FavoriteProvider> 안에서만 사용할 수 있습니다.");
  }
  return ctx;
}
