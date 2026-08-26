import { defineConfig, devices } from '@playwright/test';

// 백엔드 실행 명령 — OS에 맞는 gradlew 선택.
// 윈도우 cmd는 현재 폴더의 배치파일도 '.\' 접두어가 있어야 실행됨.
const backendCommand =
  process.platform === 'win32' ? '.\\gradlew.bat bootRun' : './gradlew bootRun';

export default defineConfig({
  testDir: './e2e',

  // 백엔드가 인메모리 DB를 공유하므로 병렬 대신 '순차' 실행이 안전
  fullyParallel: false,
  workers: 1,

  reporter: 'html',

  use: {
    baseURL: 'http://localhost:5173', // 테스트에서 page.goto('/signup') 처럼 상대경로 사용 가능
    trace: 'on-first-retry',          // 실패 시 재시도할 때 추적 기록(디버깅용)
  },

  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],

  // 테스트 시작 전 프론트·백엔드를 자동 기동하고, 끝나면 종료.
  // 이미 떠 있으면 재사용(reuseExistingServer) → 개발 중 빠르게 반복 가능.
  webServer: [
    {
      command: 'npm run dev',
      url: 'http://localhost:5173',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: backendCommand,
      cwd: '../haruverse_springboot',
      url: 'http://localhost:8080/api/health', // 헬스 엔드포인트로 준비 완료 감지
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
});
