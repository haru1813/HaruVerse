import { test, expect, type Page } from '@playwright/test';

/** 푸터가 화면에 들어오도록 맨 아래로 */
async function scrollToFooter(page: Page) {
  await page.goto('/');
  await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();
  await page.locator('footer').scrollIntoViewIfNeeded();
}

/** 푸터 안의 링크만 고른다 (헤더·본문에 같은 글자가 있을 수 있으므로) */
function footerLink(page: Page, name: string) {
  return page.locator('footer').getByRole('link', { name, exact: true });
}

test.describe('푸터 — 정보 링크', () => {
  const PAGES = [
    { label: '소개', path: '/about', heading: '소개' },
    { label: '이용약관', path: '/terms', heading: '이용약관' },
    { label: '개인정보처리방침', path: '/privacy', heading: '개인정보처리방침' },
    { label: '문의', path: '/contact', heading: '문의' },
  ];

  for (const { label, path, heading } of PAGES) {
    test(`${label} → ${path} 로 이동하고 내용이 보인다`, async ({ page }) => {
      await scrollToFooter(page);
      await footerLink(page, label).click();

      await expect(page).toHaveURL(new RegExp(`${path}$`));
      await expect(page.getByRole('heading', { name: heading })).toBeVisible();
    });
  }

  test('★SPA 이동★ 문서를 다시 받아오지 않는다 (전체 새로고침이 아님)', async ({ page }) => {
    await scrollToFooter(page);

    // 현재 문서에만 존재하는 표식을 심는다.
    // <Link href>였다면 브라우저가 문서를 새로 받아와 이 값이 사라진다.
    await page.evaluate(() => {
      (window as unknown as { __notReloaded?: boolean }).__notReloaded = true;
    });

    await footerLink(page, '소개').click();
    await expect(page.getByRole('heading', { name: '소개' })).toBeVisible();

    const survived = await page.evaluate(
      () => (window as unknown as { __notReloaded?: boolean }).__notReloaded === true,
    );
    expect(survived).toBe(true);
  });

  test('푸터에서 이동하면 새 페이지가 맨 위부터 보인다', async ({ page }) => {
    await scrollToFooter(page);
    expect(await page.evaluate(() => window.scrollY)).toBeGreaterThan(0);

    await footerLink(page, '이용약관').click();
    await expect(page.getByRole('heading', { name: '이용약관' })).toBeVisible();

    // ScrollToTop이 없으면 하단에 머문다
    expect(await page.evaluate(() => window.scrollY)).toBe(0);
  });
});

test.describe('푸터 — 탐색 링크', () => {
  test('애니메이션 → ?type=ANIME 로 필터된다', async ({ page }) => {
    await scrollToFooter(page);
    await footerLink(page, '애니메이션').click();

    await expect(page).toHaveURL(/[?&]type=ANIME/);
    await expect(page.getByRole('heading', { name: '애니메이션' })).toBeVisible();
    // 같은 경로에서 쿼리만 바뀌어도 맨 위로 올라가야 한다
    expect(await page.evaluate(() => window.scrollY)).toBe(0);
  });

  test('게임 → ?type=GAME 로 필터된다', async ({ page }) => {
    await scrollToFooter(page);
    await footerLink(page, '게임').click();

    await expect(page).toHaveURL(/[?&]type=GAME/);
    await expect(page.getByRole('heading', { name: '게임' })).toBeVisible();
  });
});

test.describe('푸터 — 외부 링크', () => {
  test('데이터 출처는 새 탭으로 열리고 rel 보안 속성이 붙는다', async ({ page }) => {
    await scrollToFooter(page);

    for (const [name, url] of [['Jikan API (애니)', 'jikan.moe'], ['RAWG (게임)', 'rawg.io']] as const) {
      const link = footerLink(page, name);
      await expect(link).toHaveAttribute('href', new RegExp(url));
      await expect(link).toHaveAttribute('target', '_blank');
      // noopener가 없으면 열린 창이 window.opener로 원래 탭을 조작할 수 있다
      await expect(link).toHaveAttribute('rel', /noopener/);
    }
  });

  test('GitHub 버튼에 실제 주소가 걸려 있다 (href="#" 아님)', async ({ page }) => {
    await scrollToFooter(page);

    const gh = page.locator('footer').getByRole('link', { name: 'GitHub' });
    await expect(gh).toHaveAttribute('href', /github\.com\/.+/);
    await expect(gh).toHaveAttribute('target', '_blank');
  });
});
