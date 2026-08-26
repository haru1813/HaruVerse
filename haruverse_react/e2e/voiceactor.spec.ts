import { test, expect, type Page } from '@playwright/test';

async function waitForCards(page: Page) {
  await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible({ timeout: 15000 });
}

test.describe('성우 도감 — 목록', () => {
  test('헤더 "성우" 탭 → /voice-actors 로 이동한다', async ({ page }) => {
    await page.goto('/');
    await waitForCards(page);

    await expect(page.getByRole('tab', { name: '성우' })).toBeEnabled();
    await page.getByRole('tab', { name: '성우' }).click();

    await expect(page).toHaveURL(/\/voice-actors/);
    await expect(page.getByRole('heading', { name: '성우', exact: true })).toBeVisible();
    await waitForCards(page);
  });

  test('맡은 배역이 많은 순으로 정렬된다', async ({ page }) => {
    await page.goto('/voice-actors');
    await waitForCards(page);

    const counts = (await page.getByText(/^\d+개 배역$/).allTextContents()).map((t) =>
      parseInt(t.replace(/\D/g, ''), 10),
    );
    expect(counts.length).toBeGreaterThan(1);
    for (let i = 1; i < counts.length; i++) {
      expect(counts[i]).toBeLessThanOrEqual(counts[i - 1]);
    }
  });

  test('헤더 검색이 성우 안에서 동작한다', async ({ page }) => {
    await page.goto('/voice-actors');
    await waitForCards(page);

    await page.getByLabel('작품 검색').fill('Kugimiya');
    await page.getByLabel('작품 검색').press('Enter');

    await expect(page).toHaveURL(/\/voice-actors\?.*q=Kugimiya/);
    await expect(page.getByRole('heading', { name: /'Kugimiya' 성우 검색 결과/ })).toBeVisible();
    await expect(page.locator('.MuiCardActionArea-root')).toHaveCount(1);
  });

  test('검색 결과 없음 → 안내 문구', async ({ page }) => {
    await page.goto('/voice-actors?q=존재하지않는성우xyz');
    await expect(page.getByText('조건에 맞는 성우가 없어요.')).toBeVisible();
  });
});

test.describe('성우 도감 — 상세', () => {
  test('성우를 누르면 맡은 캐릭터가 나온다', async ({ page }) => {
    await page.goto('/voice-actors?q=Kugimiya');
    await waitForCards(page);

    await page.locator('.MuiCardActionArea-root').first().click();

    await expect(page).toHaveURL(/\/voice-actor\/\d+/);
    await expect(page.getByRole('heading', { name: /Kugimiya/ })).toBeVisible();
    await expect(page.getByText('맡은 캐릭터')).toBeVisible();
    await waitForCards(page);
  });

  test('맡은 캐릭터를 누르면 캐릭터 상세로 간다', async ({ page }) => {
    await page.goto('/voice-actors?q=Kugimiya');
    await waitForCards(page);
    await page.locator('.MuiCardActionArea-root').first().click();
    await expect(page.getByText('맡은 캐릭터')).toBeVisible();

    await page.locator('.MuiCardActionArea-root').first().click();
    await expect(page).toHaveURL(/\/character\/\d+/);
  });

  test('잘못된 주소 → 안내', async ({ page }) => {
    await page.goto('/voice-actor/abc');
    await expect(page.getByText('잘못된 성우 주소입니다.')).toBeVisible();
  });
});

// 이 기능의 핵심 — 캐릭터에서 성우로, 성우에서 다시 캐릭터로
test.describe('★캐릭터 ↔ 성우 왕복★', () => {
  test('캐릭터 상세의 성우를 누르면 그 성우의 다른 배역을 볼 수 있다', async ({ page }) => {
    await page.goto('/characters?q=Frieren');
    await waitForCards(page);
    await page.locator('.MuiCardActionArea-root').first().click();
    await expect(page.getByRole('heading', { name: 'Frieren' })).toBeVisible();

    // 성우 칩
    const chip = page.locator('.MuiChip-root', { hasText: 'Tanezaki, Atsumi' });
    await expect(chip).toBeVisible();
    await chip.click();

    await expect(page).toHaveURL(/\/voice-actor\/\d+/);
    await expect(page.getByRole('heading', { name: 'Tanezaki, Atsumi' })).toBeVisible();
    // 이 성우가 맡은 캐릭터 중에 Frieren이 있어야 한다
    await expect(page.getByText('Frieren', { exact: true }).first()).toBeVisible();
  });

  test('성우가 없는 캐릭터는 성우 칩 자체가 없다', async ({ page }) => {
    // 'Bakery Owner'는 성우 정보가 없는 단역
    await page.goto('/characters?q=Bakery');
    await waitForCards(page);
    await page.locator('.MuiCardActionArea-root').first().click();

    await expect(page.getByRole('heading', { name: 'Bakery Owner' })).toBeVisible();
    await expect(page.locator('.MuiChip-root', { hasText: 'Tanezaki' })).toHaveCount(0);
  });
});
