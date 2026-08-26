import { test, expect, type Page } from '@playwright/test';

async function waitForCards(page: Page) {
  await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible({ timeout: 15000 });
}

test.describe('제작사 목록', () => {
  test('헤더 "제작사" 탭 → /studios 로 이동한다', async ({ page }) => {
    await page.goto('/');
    await waitForCards(page);

    await expect(page.getByRole('tab', { name: '제작사' })).toBeEnabled();
    await page.getByRole('tab', { name: '제작사' }).click();

    await expect(page).toHaveURL(/\/studios/);
    await expect(page.getByRole('heading', { name: '제작사', exact: true })).toBeVisible();
    await waitForCards(page);
  });

  test('작품이 많은 순으로 정렬된다', async ({ page }) => {
    await page.goto('/studios');
    await waitForCards(page);

    const counts = (await page.getByText(/^작품 \d+편$/).allTextContents()).map((t) =>
      parseInt(t.replace(/\D/g, ''), 10),
    );
    expect(counts.length).toBeGreaterThan(1);
    for (let i = 1; i < counts.length; i++) {
      expect(counts[i]).toBeLessThanOrEqual(counts[i - 1]);
    }
  });

  test('제작사를 누르면 그 제작사의 작품만 나온다', async ({ page }) => {
    await page.goto('/studios');
    await waitForCards(page);

    // 첫 카드의 제작사 이름
    const name = (await page.locator('.MuiCardActionArea-root').first().locator('p').first().textContent())!.trim();
    await page.locator('.MuiCardActionArea-root').first().click();

    await expect(page).toHaveURL(/[?&]studio=/);
    await expect(page.getByRole('heading', { name: `${name} 작품` })).toBeVisible();
    await expect(page.locator('.MuiChip-root', { hasText: `제작사: ${name}` })).toBeVisible();
    await waitForCards(page);
  });

  test('헤더 검색이 제작사 안에서 동작한다', async ({ page }) => {
    await page.goto('/studios');
    await waitForCards(page);

    await page.getByLabel('작품 검색').fill('Mad');
    await page.getByLabel('작품 검색').press('Enter');

    await expect(page).toHaveURL(/\/studios\?.*q=Mad/);
    await expect(page.getByRole('heading', { name: /'Mad' 제작사 검색 결과/ })).toBeVisible();
  });

  test('검색 결과 없음 → 안내 문구', async ({ page }) => {
    await page.goto('/studios?q=존재하지않는제작사xyz');
    await expect(page.getByText('조건에 맞는 제작사가 없어요.')).toBeVisible();
  });

  test('제작사 칩의 X로 필터를 해제한다', async ({ page }) => {
    await page.goto('/?studio=MAPPA');
    await expect(page.getByRole('heading', { name: 'MAPPA 작품' })).toBeVisible();

    await page.locator('.MuiChip-root', { hasText: '제작사: MAPPA' }).getByTestId('CancelIcon').click();
    await expect(page).not.toHaveURL(/[?&]studio=/);
    await expect(page.getByRole('heading', { name: '작품 도감' })).toBeVisible();
  });
});

test.describe('작품 상세 — 제작사 링크', () => {
  test('제작사 이름을 누르면 그 제작사 작품 목록으로 간다', async ({ page }) => {
    await page.goto('/work/1');
    await expect(page.getByRole('heading', { name: /Frieren/ })).toBeVisible();

    await page.getByText('Madhouse', { exact: true }).click();

    await expect(page).toHaveURL(/[?&]studio=Madhouse/);
    await expect(page.getByRole('heading', { name: 'Madhouse 작품' })).toBeVisible();
  });
});

// 제작사 필터를 넣으면서 함께 고친 버그 — 프론트에서도 확인한다
test.describe('★회귀★ 검색과 카테고리가 함께 걸린다', () => {
  test('게임 탭에서 검색하면 게임만 나온다', async ({ page }) => {
    await page.goto('/?type=GAME');
    // ★exact 필수★ 히어로 배너의 '애니메이션 · 게임 통합 도감'도 heading이라
    // 부분 일치로는 두 개가 잡혀 strict mode 위반이 난다
    await expect(page.getByRole('heading', { name: '게임', exact: true })).toBeVisible();
    await waitForCards(page);

    await page.getByLabel('작품 검색').fill('the');
    await page.getByLabel('작품 검색').press('Enter');
    await expect(page.getByRole('heading', { name: /'the' 검색 결과/ })).toBeVisible();
    await waitForCards(page);

    // 예전에는 검색어가 type을 덮어써서 애니가 나왔다.
    // 게임 카드에는 "2020년" 형태의 부제가, 애니에는 "2026 여름" 형태가 붙는다.
    const subtitles = await page.locator('.MuiCardActionArea-root').locator('span').allTextContents();
    const seasonal = subtitles.filter((t) => /(봄|여름|가을|겨울)/.test(t));
    expect(seasonal).toHaveLength(0);
  });

  test('제작사 + 종류를 함께 걸 수 있다', async ({ page }) => {
    await page.goto('/?studio=Nintendo&type=GAME');
    await expect(page.getByRole('heading', { name: 'Nintendo 작품' })).toBeVisible();
    await waitForCards(page);

    // 칩이 둘 다 떠 있어야 한다
    await expect(page.locator('.MuiChip-root', { hasText: '제작사: Nintendo' })).toBeVisible();
    await expect(page.locator('.MuiChip-root', { hasText: '게임' }).first()).toBeVisible();
  });
});
