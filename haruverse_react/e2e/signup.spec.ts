import { test, expect, type Page } from '@playwright/test';

// 매 실행마다 겹치지 않는 이메일 생성 (백엔드 DB가 재사용될 때 충돌 방지)
function uniqueEmail(): string {
  return `e2e_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}@haru.test`;
}

// 회원가입 폼 4개 필드 채우기 (반복되니 헬퍼로)
async function fillSignupForm(
  page: Page,
  opts: { email: string; nickname: string; password: string; passwordConfirm?: string },
) {
  await page.getByRole('textbox', { name: '이메일' }).fill(opts.email);
  await page.getByRole('textbox', { name: '닉네임' }).fill(opts.nickname);
  await page.getByRole('textbox', { name: '비밀번호', exact: true }).fill(opts.password);
  await page
    .getByRole('textbox', { name: '비밀번호 확인' })
    .fill(opts.passwordConfirm ?? opts.password);
}

test('회원가입 성공 → 로그인 화면으로 이동', async ({ page }) => {
  const email = uniqueEmail();

  await page.goto('/signup');
  await fillSignupForm(page, { email, nickname: '하루테스터', password: 'test1234!' });
  await page.getByRole('button', { name: '가입하기' }).click();

  // 성공 시 /login 으로 리다이렉트
  await expect(page).toHaveURL(/\/login$/);
});

test('이미 가입된 이메일 → 중복 에러 표시', async ({ page }) => {
  const email = uniqueEmail();

  // 1) 최초 가입 (성공)
  await page.goto('/signup');
  await fillSignupForm(page, { email, nickname: '중복1', password: 'test1234!' });
  await page.getByRole('button', { name: '가입하기' }).click();
  await expect(page).toHaveURL(/\/login$/);

  // 2) 같은 이메일로 재가입 → 409 → 에러 알림
  await page.goto('/signup');
  await fillSignupForm(page, { email, nickname: '중복2', password: 'test1234!' });
  await page.getByRole('button', { name: '가입하기' }).click();

  await expect(page.getByRole('alert')).toContainText('이미 사용 중인 이메일입니다.');
  await expect(page).toHaveURL(/\/signup$/); // 이동하지 않고 그대로 머무름
});

test('비밀번호 확인 불일치 → 클라이언트 검증 에러', async ({ page }) => {
  const email = uniqueEmail();

  await page.goto('/signup');
  await fillSignupForm(page, {
    email,
    nickname: '불일치',
    password: 'test1234!',
    passwordConfirm: 'different!',
  });
  await page.getByRole('button', { name: '가입하기' }).click();

  // 서버로 가지 않고 프론트 검증에서 막힘
  await expect(page.getByRole('alert')).toContainText('비밀번호가 일치하지 않습니다.');
  await expect(page).toHaveURL(/\/signup$/);
});
