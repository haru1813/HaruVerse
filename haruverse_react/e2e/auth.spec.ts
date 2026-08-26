import { test, expect } from '@playwright/test';

// 매 실행 고유 이메일 (백엔드 DB 재사용 시 충돌 방지)
function uniqueEmail(): string {
  return `auth_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}@haru.test`;
}

// JWT 인증이 '실제로' 작동하는지 검증 — API 레벨 E2E.
// (request 픽스처는 baseURL http://localhost:5173 → Vite 프록시로 /api → 백엔드 :8080)
test.describe('JWT 인증 흐름', () => {
  test('로그인으로 받은 토큰으로 보호 API 접근 → 200 + 내 정보', async ({ request }) => {
    const email = uniqueEmail();
    const password = 'test1234!';
    const nickname = '인증테스터';

    // 1) 회원가입
    const signupRes = await request.post('/api/auth/signup', {
      data: { email, password, nickname },
    });
    expect(signupRes.ok()).toBeTruthy();

    // 2) 로그인 → JWT 토큰 획득
    const loginRes = await request.post('/api/auth/login', {
      data: { email, password },
    });
    expect(loginRes.ok()).toBeTruthy();
    const { token } = await loginRes.json();
    expect(token).toBeTruthy();

    // 3) 토큰을 실어 보호 엔드포인트 호출 → 인증 통과 → 내 정보 반환
    const meRes = await request.get('/api/members/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(meRes.status()).toBe(200);
    const me = await meRes.json();
    expect(me.email).toBe(email);
    expect(me.nickname).toBe(nickname);
  });

  test('토큰 없이 보호 API 접근 → 401', async ({ request }) => {
    const res = await request.get('/api/members/me');
    expect(res.status()).toBe(401);
  });

  test('위조/유효하지 않은 토큰 → 401', async ({ request }) => {
    const res = await request.get('/api/members/me', {
      headers: { Authorization: 'Bearer invalid.forged.token' },
    });
    expect(res.status()).toBe(401);
  });

  test('공개 엔드포인트(회원가입)는 토큰 없이도 접근 가능', async ({ request }) => {
    const res = await request.post('/api/auth/signup', {
      data: { email: uniqueEmail(), password: 'test1234!', nickname: '공개' },
    });
    expect(res.ok()).toBeTruthy(); // /api/auth/** 는 permitAll
  });
});
