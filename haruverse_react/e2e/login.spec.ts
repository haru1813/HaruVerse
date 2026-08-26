import { test, expect, type Page } from '@playwright/test';

function uniqueEmail(): string {
  return `login_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}@haru.test`;
}

// UI로 회원가입까지 마친 뒤 로그인 화면(/login)에 있게 만드는 헬퍼
async function signupViaUi(page: Page, email: string, password: string) {
  await page.goto('/signup');
  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '닉네임' }).fill('로그인유저');
  await page.getByRole('textbox', { name: '비밀번호', exact: true }).fill(password);
  await page.getByRole('textbox', { name: '비밀번호 확인' }).fill(password);
  await page.getByRole('button', { name: '가입하기' }).click();
  await expect(page).toHaveURL(/\/login$/);
  // ★URL만 보고 다음 단계로 넘어가면 안 된다★
  //   toHaveURL은 주소가 바뀌는 즉시 통과하지만, 그 시점에 Login 컴포넌트는
  //   아직 마운트 중일 수 있다. 그 상태에서 fill()하면 DOM 값만 채워지고
  //   React state에는 반영되지 않아, 제출 시 빈 값으로 검증에 걸린다.
  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();
}

test('로그인 성공 → 홈 이동 + JWT 토큰 저장', async ({ page }) => {
  const email = uniqueEmail();
  const password = 'test1234!';

  await signupViaUi(page, email, password);

  // 로그인 폼 입력 (헤더의 '로그인' 버튼과 겹치지 않게 form 안의 버튼을 지정)
  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '비밀번호' }).fill(password);
  await page.locator('form').getByRole('button', { name: '로그인' }).click();

  // 홈으로 이동
  await expect(page).toHaveURL(/\/$/);

  // localStorage에 JWT 토큰이 저장됐는지 확인
  const token = await page.evaluate(() => localStorage.getItem('haruverse_token'));
  expect(token).toBeTruthy();
});

test('잘못된 비밀번호 → 401 에러 표시', async ({ page }) => {
  const email = uniqueEmail();
  const password = 'test1234!';

  await signupViaUi(page, email, password);

  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '비밀번호' }).fill('wrong-password');
  await page.locator('form').getByRole('button', { name: '로그인' }).click();

  await expect(page.getByRole('alert')).toContainText('이메일 또는 비밀번호가 올바르지 않습니다.');
  await expect(page).toHaveURL(/\/login$/); // 실패 시 그대로 머무름
});
