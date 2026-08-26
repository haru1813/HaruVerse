import { useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useAuth } from "./AuthContext";
import { FavoriteContext } from "./FavoriteContext";
import { addFavorite, fetchFavoriteIds, removeFavorite } from "../features/favorite/api";

/**
 * 찜 상태를 앱 전체에 공급한다.
 *
 * <p><b>왜 전역 상태인가</b>
 * 하트는 홈 그리드·상세 페이지·마이페이지 세 곳에 동시에 존재한다.
 * 각 화면이 따로 들고 있으면, 상세에서 찜을 풀고 뒤로 갔을 때 홈의 하트가
 * 아직 켜져 있는 식으로 어긋난다. 한 곳에서 관리해야 한다.
 *
 * <p>반드시 &lt;AuthProvider&gt; 안쪽에 놓아야 한다 (useAuth를 쓴다).
 */
function FavoriteProvider({ children }: { children: ReactNode }) {
  const { isLoggedIn } = useAuth();

  const [favoriteIds, setFavoriteIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(isLoggedIn);

  // ★로그인 상태가 바뀌는 순간의 처리 — useEffect가 아니라 렌더 중에 조정한다★
  //   useEffect로 비우면 (1) 로그아웃 직후 한 프레임 동안 남의 하트가 켜진 채 보이고
  //   (2) react-hooks/set-state-in-effect 린트에도 걸린다.
  //   React가 공식적으로 권하는 "렌더 도중 state 조정" 패턴이다.
  const [syncedLoggedIn, setSyncedLoggedIn] = useState(isLoggedIn);
  if (isLoggedIn !== syncedLoggedIn) {
    setSyncedLoggedIn(isLoggedIn);
    if (!isLoggedIn) setFavoriteIds(new Set()); // 로그아웃 → 즉시 비움
    setLoading(isLoggedIn);
  }

  // 로그인 상태가 되면 내 찜 id를 한 번에 받아온다
  useEffect(() => {
    if (!isLoggedIn) return;
    let alive = true;

    fetchFavoriteIds()
      .then((ids) => {
        if (alive) setFavoriteIds(new Set(ids));
      })
      .catch(() => {
        // 실패해도 화면은 굴러가야 한다 — 하트가 안 칠해질 뿐
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [isLoggedIn]);

  /**
   * 찜/해제 토글 — <b>낙관적 갱신(optimistic update)</b>.
   *
   * <p>서버 응답을 기다린 뒤 하트를 바꾸면 클릭과 반응 사이에 눈에 띄는 지연이 생긴다.
   * 그래서 먼저 화면을 바꾸고 요청을 보낸 다음, 실패하면 되돌린다.
   */
  const toggleFavorite = useCallback(
    async (workId: number) => {
      const wasFavorite = favoriteIds.has(workId);

      const flip = (add: boolean) =>
        setFavoriteIds((prev) => {
          const next = new Set(prev);
          if (add) next.add(workId);
          else next.delete(workId);
          return next;
        });

      flip(!wasFavorite); // ① 화면 먼저
      try {
        await (wasFavorite ? removeFavorite(workId) : addFavorite(workId)); // ② 서버
      } catch (e) {
        flip(wasFavorite); // ③ 실패하면 원상 복구
        throw e;
      }
    },
    [favoriteIds],
  );

  const isFavorite = useCallback((workId: number) => favoriteIds.has(workId), [favoriteIds]);

  // value를 매 렌더 새로 만들면 하위 전체가 다시 그려진다 → useMemo로 고정
  const value = useMemo(
    () => ({ favoriteIds, isFavorite, toggleFavorite, loading }),
    [favoriteIds, isFavorite, toggleFavorite, loading],
  );

  return <FavoriteContext.Provider value={value}>{children}</FavoriteContext.Provider>;
}

export default FavoriteProvider;
