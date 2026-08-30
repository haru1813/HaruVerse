// Vuetify 설정 — 서비스 앱(haruverse_react)의 팔레트를 그대로 가져온다.
//
// ★한 곳만 일부러 다르게 뒀다: 배경색★
//   서비스 앱은 흰 배경이다(작품 카드가 배경과 이어져 보이도록 만든 선택).
//   관리자 콘솔은 반대다. 표·폼·통계 카드가 촘촘히 놓이므로
//   배경이 옅은 회색이어야 카드 경계가 읽힌다.
//   같은 팔레트를 쓰되 이 한 값만 다르다.

import "@mdi/font/css/materialdesignicons.css";
import "vuetify/styles";

import { createVuetify } from "vuetify";

export default createVuetify({
  theme: {
    defaultTheme: "haruverseAdmin",
    themes: {
      haruverseAdmin: {
        dark: false,
        colors: {
          primary: "#2563eb", // 서비스 앱 primary
          secondary: "#38bdf8", // 시안 — HaruVerse 포인트 색
          navy: "#1b2a4a", // 헤더 바 (서비스 앱 AppBar와 동일)
          background: "#f4f6fa", // ★여기만 서비스 앱과 다르다★
          surface: "#ffffff",
          error: "#b91c1c",
          success: "#047857",
          warning: "#b45309",
          info: "#2563eb",
        },
      },
    },
  },
  defaults: {
    // 관리 화면은 정보 밀도가 높다. 기본 여백을 줄이고 테두리로 구분한다.
    VCard: { elevation: 0, rounded: "lg", border: true },
    VTextField: { variant: "outlined", density: "comfortable" },
    VSelect: { variant: "outlined", density: "comfortable" },
    VBtn: { variant: "flat" },
  },
});
