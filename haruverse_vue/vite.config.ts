import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vuetify from "vite-plugin-vuetify";

// ★/api 프록시★
//   관리자 콘솔은 API를 항상 같은 오리진("/api/...")으로 부른다(src/lib/api.ts 참고).
//   개발에서는 여기 프록시가, 운영에서는 컨테이너의 nginx가 그 경로를 백엔드로 넘긴다.
//   덕분에 백엔드에 CORS 설정을 만들 필요가 없다.
//
//   target 이 8080 인 이유: 로컬에서 스프링을 직접 띄운 경우다.
//   도커로 띄웠다면 docker-compose.yml 의 백엔드 포트에 맞춰 바꾼다.
export default defineConfig({
  plugins: [
    vue(),
    // 쓰는 컴포넌트만 번들에 넣는다 (Vuetify 전체는 무겁다)
    vuetify({ autoImport: true }),
  ],
  server: {
    port: 5310, // 서비스 앱(5173)과 겹치지 않게
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        // 수집 API는 몇 분씩 걸린다 — 기본 타임아웃으로는 끊긴다
        timeout: 600000,
        proxyTimeout: 600000,
      },
    },
  },
});
