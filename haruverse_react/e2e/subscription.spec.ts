import { test, expect, type Page } from '@playwright/test';

/**
 * 채널 구독.
 *
 * <p>구독은 찜과 데이터 모양이 같지만(회원–작품) <b>뜻이 다르다</b>.
 * 찜 = "이 작품이 좋다"(도감) / 구독 = "이 게시판 글을 읽겠다"(커뮤니티).
 * 그래서 <b>따로 저장되고 서로를 건드리지 않아야 한다</b> — 아래에서 그걸 검증한다.
 */

function uniqueEmail(): string {
  return `sub_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}@haru.test`;
}

const PASSWORD = 'test1234!';

async function signupAndLogin(page: Page) {
  const email = uniqueEmail();
  const nickname = `구독_${Date.now() % 100000}`;

  await page.goto('/signup');
  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '닉네임' }).fill(nickname);
  await page.getByRole('textbox', { name: '비밀번호', exact: true }).fill(PASSWORD);
  await page.getByRole('textbox', { name: '비밀번호 확인' }).fill(PASSWORD);
  await page.getByRole('button', { name: '가입하기' }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();

  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '비밀번호' }).fill(PASSWORD);
  await page.locator('form').getByRole('button', { name: '로그인' }).click();
  await expect(page).toHaveURL(/\/$/);
  return { email, nickname };
}

test.describe('구독 — 진입', () => {
  test('게시판에 구독 버튼이 있다', async ({ page }) => {
    await page.goto('/work/1/posts');
    await expect(page.getByRole('button', { name: '구독', exact: true })).toBeVisible();
  });

  test('★비로그인이 구독을 누르면 로그인 화면으로★ (버튼을 숨기지 않는다)', async ({ page }) => {
    await page.goto('/work/1/posts');
    await page.getByRole('button', { name: '구독', exact: true }).click();

    await expect(page).toHaveURL(/\/login$/);
  });

  test('글 상세 사이드바에도 구독 버튼이 있다', async ({ page }) => {
    await page.goto('/post/1');
    await expect(page.getByText('이 게시판의 다른 글')).toBeVisible();
    await expect(page.getByRole('button', { name: '구독', exact: true })).toBeVisible();
  });
});

test.describe('구독 — 동작', () => {
  test('구독하면 버튼이 "구독 중"으로 바뀐다', async ({ page }) => {
    await signupAndLogin(page);
    await page.goto('/work/1/posts');

    await page.getByRole('button', { name: '구독', exact: true }).click();
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();
  });

  test('★구독은 새로고침 후에도 유지된다★ (서버에 저장된다)', async ({ page }) => {
    await signupAndLogin(page);
    await page.goto('/work/1/posts');
    await page.getByRole('button', { name: '구독', exact: true }).click();
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();

    await page.reload();
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();
  });

  test('다시 누르면 구독이 해제된다', async ({ page }) => {
    await signupAndLogin(page);
    await page.goto('/work/1/posts');

    await page.getByRole('button', { name: '구독', exact: true }).click();
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();

    await page.getByRole('button', { name: '구독 중' }).click();
    await expect(page.getByRole('button', { name: '구독', exact: true })).toBeVisible();
  });

  test('★구독 상태는 화면 사이에 공유된다★ (게시판에서 구독 → 글 상세에도 반영)', async ({ page }) => {
    await signupAndLogin(page);
    await page.goto('/work/1/posts');
    await page.getByRole('button', { name: '구독', exact: true }).click();
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();

    // 전역 Context가 없으면 여기서 다시 "구독"으로 보인다
    await page.goto('/post/1');
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();
  });
});

test.describe('구독 — 커뮤니티 첫 화면', () => {
  test('구독하면 "내 구독 채널"이 생긴다', async ({ page }) => {
    await signupAndLogin(page);

    // 구독 전에는 섹션이 없다
    await page.goto('/community');
    await expect(page.getByText('내 구독 채널')).toHaveCount(0);

    await page.goto('/work/1/posts');
    await page.getByRole('button', { name: '구독', exact: true }).click();
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();

    await page.goto('/community');
    await expect(page.getByText('내 구독 채널')).toBeVisible();
    // 섹션이 생기면 아래 목록 제목이 "커뮤니티" → "전체 채널" 로 바뀐다
    await expect(page.getByText('전체 채널')).toBeVisible();
  });

  test('★구독을 풀면 섹션에서 즉시 사라진다★ (새로고침 없이)', async ({ page }) => {
    await signupAndLogin(page);
    await page.goto('/work/1/posts');
    await page.getByRole('button', { name: '구독', exact: true }).click();
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();

    await page.goto('/community');
    await expect(page.getByText('내 구독 채널')).toBeVisible();

    // 구독 섹션 카드의 버튼을 눌러 해제 — 목록이 다시 조회돼야 한다
    await page.getByRole('button', { name: '구독 중' }).first().click();
    await expect(page.getByText('내 구독 채널')).toHaveCount(0);
  });

  test('비로그인에게는 구독 섹션이 없다', async ({ page }) => {
    await page.goto('/community');

    await expect(page.getByText('전체 채널')).toHaveCount(0);
    await expect(page.getByText('내 구독 채널')).toHaveCount(0);
  });
});

test.describe('★구독과 찜은 별개★', () => {
  test('구독해도 찜은 걸리지 않는다', async ({ page }) => {
    await signupAndLogin(page);
    await page.goto('/post/1');
    await expect(page.getByText('이 게시판의 다른 글')).toBeVisible();

    await page.getByRole('button', { name: '구독', exact: true }).click();
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();

    // 찜 버튼은 그대로 "찜하기" — 하나로 합쳤다면 여기서 "찜 해제"가 된다
    await expect(page.getByRole('button', { name: '찜하기' })).toBeVisible();
  });

  test('찜을 풀어도 구독은 남는다', async ({ page }) => {
    await signupAndLogin(page);
    await page.goto('/post/1');
    await expect(page.getByText('이 게시판의 다른 글')).toBeVisible();

    await page.getByRole('button', { name: '구독', exact: true }).click();
    await page.getByRole('button', { name: '찜하기' }).click();
    await expect(page.getByRole('button', { name: '찜 해제' })).toBeVisible();

    await page.getByRole('button', { name: '찜 해제' }).click();
    await expect(page.getByRole('button', { name: '찜하기' })).toBeVisible();

    // ★핵심★ 찜을 풀어도 구독은 그대로
    await expect(page.getByRole('button', { name: '구독 중' })).toBeVisible();
  });
});
