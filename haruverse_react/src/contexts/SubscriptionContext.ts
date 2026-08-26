// 구독 컨텍스트 '정의' 파일 — Context 객체와 훅만 둔다.
// (Provider는 SubscriptionProvider.tsx로 분리 — FavoriteContext와 같은 이유로 Fast Refresh 보호.
//  한 파일에서 컴포넌트와 컴포넌트가 아닌 것을 함께 export하면 Vite가 갱신을 포기하고 새로고침한다)

import { createContext, useContext } from "react";

export type SubscriptionContextValue = {
  /** 내가 구독한 채널(작품) id 집합 — 카드마다 O(1)로 조회하려고 Set */
  subscribedIds: Set<number>;
  isSubscribed: (workId: number) => boolean;
  /** 구독/해제를 뒤집는다. 화면은 즉시 반영되고, 실패하면 자동으로 되돌아간다 */
  toggleSubscription: (workId: number) => Promise<void>;
  /** 최초 로딩 중인지 (버튼 글자가 깜빡이지 않게 하려고 노출) */
  loading: boolean;
};

export const SubscriptionContext = createContext<SubscriptionContextValue | undefined>(undefined);

export function useSubscription(): SubscriptionContextValue {
  const ctx = useContext(SubscriptionContext);
  if (!ctx) {
    throw new Error("useSubscription은 <SubscriptionProvider> 안에서만 사용할 수 있습니다.");
  }
  return ctx;
}
