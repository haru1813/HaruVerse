// 라우팅과 진입 통제.
//
// ★여기서 막는 건 '화면'이지 '권한'이 아니다★
//   가드는 토큰을 서명 검증 없이 읽는다(lib/auth.ts 참고).
//   개발자 도구로 localStorage를 고쳐 이 가드를 통과할 수 있다.
//   그래도 문제가 없는 이유는, 통과한 화면이 부르는 모든 API가
//   백엔드에서 hasRole("ADMIN")으로 다시 검사되기 때문이다.
//   가드의 목적은 '권한 없는 사람에게 빈 화면 대신 안내를 보여주는 것'이다.

import { createRouter, createWebHistory } from "vue-router";
import { isAdmin, isLoggedIn } from "../lib/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: () => import("../views/LoginView.vue"),
      meta: { public: true },
    },
    {
      path: "/",
      component: () => import("../layouts/AdminLayout.vue"),
      children: [
        {
          path: "",
          name: "dashboard",
          component: () => import("../views/DashboardView.vue"),
          meta: { title: "대시보드" },
        },
        {
          path: "collect",
          name: "collect",
          component: () => import("../views/CollectView.vue"),
          meta: { title: "수집 · 색인" },
        },
      ],
    },
    // 오타나 삭제된 주소로 들어오면 대시보드로. 관리 도구에 404 화면까지는 과하다.
    { path: "/:pathMatch(.*)*", redirect: "/" },
  ],
});

router.beforeEach((to) => {
  if (to.meta.public) {
    // 이미 관리자로 로그인한 채 /login 에 들어오면 대시보드로 되돌린다
    return isAdmin() ? { name: "dashboard" } : true;
  }

  if (!isLoggedIn()) {
    // ★어디로 가려 했는지 기억한다★ 로그인 후 그 자리로 돌려보내기 위해서다.
    //   토큰 만료로 튕긴 경우에 특히 필요하다 — 하던 일을 잃지 않는다.
    return { name: "login", query: { redirect: to.fullPath } };
  }

  if (!isAdmin()) {
    // 로그인은 했지만 일반 회원이다. 로그인 화면으로 보내되 이유를 알려준다.
    return { name: "login", query: { denied: "1" } };
  }

  return true;
});

export default router;
