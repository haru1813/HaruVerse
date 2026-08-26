import { test, expect, type Page } from '@playwright/test';

// 매 실행 고유 이메일 (백엔드 DB가 파일 기반이라 계정이 누적됨 → 충돌 방지)
function uniqueEmail(): string {
  return `fav_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}@haru.test`;
}

const NICKNAME = '하루찜';
const PASSWORD = 'test1234!';

async function signupAndLogin(page: Page, email: string) {
  await page.goto('/signup');
  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '닉네임' }).fill(NICKNAME);
  await page.getByRole('textbox', { name: '비밀번호', exact: true }).fill(PASSWORD);
  await page.getByRole('textbox', { name: '비밀번호 확인' }).fill(PASSWORD);
  await page.getByRole('button', { name: '가입하기' }).click();
  await expect(page).toHaveURL(/\/login$/);
  // URL만 보고 넘어가면 React가 아직 렌더 전이라 fill이 state에 반영되지 않는다
  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();

  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '비밀번호' }).fill(PASSWORD);
  await page.locator('form').getByRole('button', { name: '로그인' }).click();
  await expect(page).toHaveURL(/\/$/);
}

/** 홈이 카드까지 실제로 그려질 때까지 기다린다 */
async function waitForGrid(page: Page) {
  await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();
}

/** 첫 카드의 제목 텍스트 */
function firstCardTitle(page: Page) {
  return page.locator('.MuiCardActionArea-root').first().locator('p').first();
}

test.describe('찜하기 — 비로그인', () => {
  test('하트를 누르면 로그인 화면으로 보낸다 (버튼을 숨기지 않는다)', async ({ page }) => {
    await page.goto('/');
    await waitForGrid(page);

    // 비로그인이어도 하트는 보여야 한다
    await expect(page.getByRole('button', { name: '찜하기' }).first()).toBeVisible();
    await page.getByRole('button', { name: '찜하기' }).first().click();

    await expect(page).toHaveURL(/\/login$/);
  });

  test('하트 클릭이 상세 이동으로 새지 않는다', async ({ page }) => {
    await page.goto('/');
    await waitForGrid(page);

    await page.getByRole('button', { name: '찜하기' }).first().click();
    // 카드 클릭(=/work/:id)으로 전파되면 안 된다
    await expect(page).not.toHaveURL(/\/work\/\d+/);
  });
});

test.describe('찜하기 — 로그인', () => {
  test('찜 → 하트가 채워지고 마이페이지 목록에 나온다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());
    await waitForGrid(page);

    const title = (await firstCardTitle(page).textContent())!.trim();
    await page.getByRole('button', { name: '찜하기' }).first().click();

    // 낙관적 갱신 → 하트가 즉시 '찜 해제'로 바뀐다
    await expect(page.getByRole('button', { name: '찜 해제' })).toHaveCount(1);

    await page.goto('/mypage');
    await expect(page.getByRole('heading', { name: /내가 찜한 작품/ })).toBeVisible();
    await expect(page.getByText(title, { exact: true })).toBeVisible();
  });

  test('찜 상태가 새로고침 후에도 유지된다 (서버에 저장됨)', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());
    await waitForGrid(page);

    await page.getByRole('button', { name: '찜하기' }).first().click();
    await expect(page.getByRole('button', { name: '찜 해제' })).toHaveCount(1);

    await page.reload();
    await waitForGrid(page);
    // localStorage가 아니라 GET /api/favorites/ids 로 복원된다
    await expect(page.getByRole('button', { name: '찜 해제' })).toHaveCount(1);
  });

  test('★멱등★ 같은 카드를 두 번 눌러 찜 → 해제하면 목록이 빈다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());
    await waitForGrid(page);

    await page.getByRole('button', { name: '찜하기' }).first().click();
    await expect(page.getByRole('button', { name: '찜 해제' })).toHaveCount(1);

    await page.getByRole('button', { name: '찜 해제' }).click();
    await expect(page.getByRole('button', { name: '찜 해제' })).toHaveCount(0);

    await page.goto('/mypage');
    await expect(page.getByText('아직 찜한 작품이 없어요.')).toBeVisible();
  });

  test('마이페이지에서 해제하면 카드가 즉시 사라진다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());
    await waitForGrid(page);

    await page.getByRole('button', { name: '찜하기' }).first().click();
    await expect(page.getByRole('button', { name: '찜 해제' })).toHaveCount(1);

    await page.goto('/mypage');
    await expect(page.locator('.MuiCardActionArea-root')).toHaveCount(1);

    // 서버 재조회 없이 컨텍스트만으로 사라져야 한다
    await page.getByRole('button', { name: '찜 해제' }).click();
    await expect(page.locator('.MuiCardActionArea-root')).toHaveCount(0);
    await expect(page.getByText('아직 찜한 작품이 없어요.')).toBeVisible();
  });

  test('상세 페이지의 찜하기 버튼이 동작한다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());
    await page.goto('/work/1');
    await expect(page.getByRole('heading', { name: /Frieren/ })).toBeVisible();

    await page.getByRole('button', { name: '찜하기' }).click();
    await expect(page.getByRole('button', { name: '찜 해제' })).toBeVisible();

    await page.goto('/mypage');
    await expect(page.locator('.MuiCardActionArea-root')).toHaveCount(1);
  });

  test('로그아웃하면 하트가 모두 빈 상태로 돌아간다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail());
    await waitForGrid(page);

    await page.getByRole('button', { name: '찜하기' }).first().click();
    await expect(page.getByRole('button', { name: '찜 해제' })).toHaveCount(1);

    await page.getByRole('button', { name: '계정 메뉴' }).click();
    await page.getByRole('menuitem', { name: '로그아웃' }).click();
    await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();

    // 남의 찜이 남아 보이면 안 된다
    await expect(page.getByRole('button', { name: '찜 해제' })).toHaveCount(0);
  });
});
