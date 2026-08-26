import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // 개발 서버 프록시 — 브라우저는 같은 출처(:5173)로 요청하고,
  // Vite가 몰래 백엔드(:8080)로 전달 → CORS 문제 없이 API 호출 가능
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
