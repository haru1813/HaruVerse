import { useEffect } from "react";
import { useLocation } from "react-router-dom";

/**
 * 라우트가 바뀌면 화면 맨 위로 올린다.
 *
 * <p><b>왜 필요한가</b>
 * 브라우저는 &lt;a&gt;로 문서를 새로 열 때 자동으로 맨 위로 가지만,
 * SPA는 문서를 갈아끼우지 않고 컴포넌트만 바꾼다. 그래서 스크롤 위치가 그대로 남는다.
 * 푸터(페이지 맨 아래)의 링크를 눌렀을 때 새 페이지가 하단부터 보이는 문제가 이것이다.
 *
 * <p><b>pathname만 보는 이유</b>
 * search(?type=GAME&page=2)까지 감시하면 홈의 검색·필터 조작마다 스크롤이 튄다.
 * 홈은 필요한 시점에 스스로 window.scrollTo를 호출하고 있다.
 */
function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null; // 화면에 아무것도 그리지 않는 '동작 전용' 컴포넌트
}

export default ScrollToTop;
