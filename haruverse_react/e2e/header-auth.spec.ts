import { test, expect, type Page } from '@playwright/test';

// 매 실행 고유 이메일 (백엔드 DB가 파일 기반이라 계정이 누적됨 → 충돌 방지)
function uniqueEmail(): string {
  return `header_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}@haru.test`;
}

const NICKNAME = '하루헤더';
const PASSWORD = 'test1234!';

// 회원가입 → 로그인까지 마치고 홈에 있게 만드는 헬퍼
async function signupAndLogin(page: Page, email: string) {
  await page.goto('/signup');
  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '닉네임' }).fill(NICKNAME);
  await page.getByRole('textbox', { name: '비밀번호', exact: true }).fill(PASSWORD);
  await page.getByRole('textbox', { name: '비밀번호 확인' }).fill(PASSWORD);
  await page.getByRole('button', { name: '가입하기' }).click();
  await expect(page).toHaveURL(/\/login$/);
  // ★URL만 보고 다음 단계로 넘어가면 안 된다★
  //   toHaveURL은 주소가 바뀌는 즉시 통과하지만, 그 시점에 Login 컴포넌트는
  //   아직 마운트 중일 수 있다. 그 상태에서 fill()하면 DOM 값만 채워지고
  //   React state에는 반영되지 않아, 제출 시 빈 값으로 검증에 걸린다.
  //   → 화면이 실제로 준비된 것을 확인하고 진행한다.
  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();

  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '비밀번호' }).fill(PASSWORD);
  await page.locator('form').getByRole('button', { name: '로그인' }).click();
  await expect(page).toHaveURL(/\/$/);
}

test.describe('헤더 로그인 상태 반영', () => {
  test('비로그인 → 헤더에 "로그인" 버튼이 보인다', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();
    await expect(page.getByRole('button', { name: '계정 메뉴' })).toHaveCount(0);
  });

  test('로그인 → 헤더가 닉네임/계정 메뉴로 바뀐다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());

    // 로그인 버튼은 사라지고 계정 메뉴가 등장
    await expect(page.getByRole('button', { name: '계정 메뉴' })).toBeVisible();
    await expect(page.getByRole('button', { name: '로그인' })).toHaveCount(0);
    // 닉네임 노출
    await expect(page.getByRole('button', { name: '계정 메뉴' })).toContainText(NICKNAME);
  });

  test('계정 메뉴 → 마이페이지 이동 → 내 정보 표시', async ({ page }) => {
    const email = uniqueEmail();
    await signupAndLogin(page, email);

    await page.getByRole('button', { name: '계정 메뉴' }).click();
    await page.getByRole('menuitem', { name: '마이페이지' }).click();

    await expect(page).toHaveURL(/\/mypage$/);
    // 보호 API(/api/members/me) 응답이 화면에 렌더되는지
    // (이메일은 프로필 요약과 상세 목록 두 곳에 나오므로 first()로 하나만 확인)
    await expect(page.getByText(email).first()).toBeVisible();
    await expect(page.getByText('JWT 인증됨')).toBeVisible();
  });

  test('로그아웃 → 헤더가 "로그인"으로 복귀 + 토큰 제거', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());

    await page.getByRole('button', { name: '계정 메뉴' }).click();
    await page.getByRole('menuitem', { name: '로그아웃' }).click();

    await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();
    await expect(page.getByRole('button', { name: '계정 메뉴' })).toHaveCount(0);

    const token = await page.evaluate(() => localStorage.getItem('haruverse_token'));
    expect(token).toBeNull();
  });

  test('새로고침해도 로그인 상태가 유지된다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());

    await page.reload();

    // localStorage에서 복원되어 헤더가 그대로 로그인 상태
    await expect(page.getByRole('button', { name: '계정 메뉴' })).toBeVisible();
    await expect(page.getByRole('button', { name: '계정 메뉴' })).toContainText(NICKNAME);
  });

  test('비로그인으로 /mypage 직접 접근 → /login 으로 리다이렉트', async ({ page }) => {
    await page.goto('/mypage');
    await expect(page).toHaveURL(/\/login$/);
  });
});
