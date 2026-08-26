import { useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useAuth } from "./AuthContext";
import { SubscriptionContext } from "./SubscriptionContext";
import { fetchSubscriptionIds, subscribe, unsubscribe } from "../features/community/subscriptionApi";

/**
 * 구독 상태를 앱 전체에 공급한다.
 *
 * <p><b>왜 전역 상태인가</b>
 * 구독 버튼은 게시판 목록·글 상세 사이드바·커뮤니티 첫 화면 세 곳에 동시에 있다.
 * 각 화면이 따로 들고 있으면, 글 상세에서 구독을 풀고 뒤로 갔을 때 커뮤니티 카드가
 * 아직 "구독 중"으로 남는 식으로 어긋난다. (찜 하트와 같은 문제)
 *
 * <p>반드시 &lt;AuthProvider&gt; 안쪽에 놓아야 한다 (useAuth를 쓴다).
 */
function SubscriptionProvider({ children }: { children: ReactNode }) {
  const { isLoggedIn } = useAuth();

  const [subscribedIds, setSubscribedIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(isLoggedIn);

  // ★로그인 상태가 바뀌는 순간의 처리 — useEffect가 아니라 렌더 중에 조정한다★
  //   useEffect로 비우면 (1) 로그아웃 직후 한 프레임 동안 남의 구독 상태가 보이고
  //   (2) react-hooks/set-state-in-effect 린트에도 걸린다.
  const [syncedLoggedIn, setSyncedLoggedIn] = useState(isLoggedIn);
  if (isLoggedIn !== syncedLoggedIn) {
    setSyncedLoggedIn(isLoggedIn);
    if (!isLoggedIn) setSubscribedIds(new Set()); // 로그아웃 → 즉시 비움
    setLoading(isLoggedIn);
  }

  // 로그인 상태가 되면 내 구독 id를 한 번에 받아온다
  useEffect(() => {
    if (!isLoggedIn) return;
    let alive = true;

    fetchSubscriptionIds()
      .then((ids) => {
        if (alive) setSubscribedIds(new Set(ids));
      })
      .catch(() => {
        // 실패해도 화면은 굴러가야 한다 — 버튼이 "구독" 상태로 보일 뿐
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [isLoggedIn]);

  /**
   * 구독/해제 토글 — <b>낙관적 갱신</b>.
   *
   * <p>서버 응답을 기다린 뒤 버튼을 바꾸면 클릭과 반응 사이에 지연이 보인다.
   * 먼저 화면을 바꾸고 요청을 보낸 다음, 실패하면 되돌린다.
   */
  const toggleSubscription = useCallback(
    async (workId: number) => {
      const wasSubscribed = subscribedIds.has(workId);

      const flip = (add: boolean) =>
        setSubscribedIds((prev) => {
          const next = new Set(prev);
          if (add) next.add(workId);
          else next.delete(workId);
          return next;
        });

      flip(!wasSubscribed); // ① 화면 먼저
      try {
        await (wasSubscribed ? unsubscribe(workId) : subscribe(workId)); // ② 서버
      } catch (e) {
        flip(wasSubscribed); // ③ 실패하면 원상 복구
        throw e;
      }
    },
    [subscribedIds],
  );

  const isSubscribed = useCallback((workId: number) => subscribedIds.has(workId), [subscribedIds]);

  // value를 매 렌더 새로 만들면 하위 전체가 다시 그려진다 → useMemo로 고정
  const value = useMemo(
    () => ({ subscribedIds, isSubscribed, toggleSubscription, loading }),
    [subscribedIds, isSubscribed, toggleSubscription, loading],
  );

  return <SubscriptionContext.Provider value={value}>{children}</SubscriptionContext.Provider>;
}

export default SubscriptionProvider;
