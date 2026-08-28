import { useEffect, useRef, useState } from "react";
import { fetchSuggestions } from "../features/work/api";
import type { Suggestion } from "../features/work/api";

/** 이 글자 수부터 후보를 띄운다 — 한 글자에 반응하면 거의 전체가 뜬다 */
const MIN_LENGTH = 2;
/** 타이핑이 멈춘 뒤 이만큼 기다렸다 요청한다 */
const DEBOUNCE_MS = 250;

/** 어떤 검색어의 결과인지 함께 들고 있는다 — 아래 '묵은 결과' 설명 참고 */
type Result = { query: string; items: Suggestion[] };

/**
 * 검색창 자동완성.
 *
 * <p>세 가지를 처리한다. 셋 다 없으면 눈에 띄는 버그가 된다.
 *
 * <p><b>① 디바운스</b>
 * 타이핑마다 요청하면 "frieren" 일곱 글자에 요청이 일곱 번 나간다.
 * 멈춘 뒤 250ms 기다렸다 한 번만 보낸다.
 *
 * <p><b>② ★경쟁 상태(race)★</b>
 * 요청이 보낸 순서대로 도착하지 않는다. "fr" 요청이 느리고 "fri" 요청이 빨랐다면,
 * 나중에 도착한 "fr" 결과가 <b>이미 그려진 "fri" 결과를 덮어쓴다.</b>
 * 사용자 눈에는 "글자를 더 쳤는데 후보가 이상해지는" 화면이 된다.
 * → 요청마다 번호를 매겨, <b>가장 마지막에 보낸 요청의 응답만</b> 반영한다.
 *
 * <p><b>③ 묵은 결과가 비치는 것</b>
 * 결과를 검색어와 <b>함께</b> 보관하고, 지금 검색어와 일치할 때만 보여준다.
 * 안 그러면 "fri"를 지우고 "zel"을 치는 순간, 새 응답이 오기 전 250ms 동안
 * 프리렌 후보가 그대로 떠 있다.
 *
 * <p><b>★조건이 안 맞을 때 state를 비우지 않는다★</b>
 * effect 안에서 {@code setItems([])} 를 부르면 렌더가 한 번 더 돈다.
 * 상태를 지우는 대신 <b>렌더 시점에 걸러내면</b> 같은 결과를 렌더 한 번으로 얻는다.
 */
export function useSuggestions(keyword: string, enabled = true) {
  const [result, setResult] = useState<Result>({ query: "", items: [] });
  const latestRequest = useRef(0);

  const query = keyword.trim();
  const active = enabled && query.length >= MIN_LENGTH;

  useEffect(() => {
    if (!active) return; // 요청만 보내지 않는다 (state는 건드리지 않는다)

    const requestId = ++latestRequest.current;

    const timer = setTimeout(async () => {
      const items = await fetchSuggestions(query);
      // 내가 마지막 요청이 아니면 버린다 (뒤늦게 온 옛 응답이 화면을 덮지 않게)
      if (requestId === latestRequest.current) setResult({ query, items });
    }, DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [query, active]);

  /** 후보를 즉시 감춘다 — 검색을 실행했거나 창을 닫을 때 */
  const clear = () => {
    latestRequest.current++; // 진행 중인 요청의 결과도 무효화
    setResult({ query: "", items: [] });
  };

  // 지금 검색어의 결과일 때만 보여준다
  const items = active && result.query === query ? result.items : [];

  return { items, clear };
}
