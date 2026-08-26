import { test, expect, type Page } from '@playwright/test';

/**
 * 게임 플랫폼 표시.
 *
 * <p>RAWG의 `parent_platforms`(PlayStation·Xbox·Nintendo…)를 담는다.
 * 기종별 `platforms`(PS4·PS5·Xbox One…)를 쓰면 칩이 열 개씩 붙는다.
 *
 * <p>★애니에는 플랫폼이 없다★ — 빈 배열이라 줄 자체가 나오지 않아야 한다.
 */

/** 플랫폼이 하나라도 있는 게임의 id를 찾는다 (특정 작품에 묶이지 않게) */
async function findGameWithPlatforms(page: Page): Promise<number> {
  return page.evaluate(async () => {
    const res = await fetch('/api/works?type=GAME&size=40');
    const data = await res.json();
    const game = data.content.find((w: { platforms: string[] }) => w.platforms.length > 0);
    if (!game) throw new Error('플랫폼이 있는 게임이 없습니다 — RAWG 수집이 필요합니다');
    return game.id as number;
  });
}

test.describe('플랫폼 — API', () => {
  test('게임 응답에 platforms가 담긴다', async ({ page }) => {
    await page.goto('/');
    const platforms = await page.evaluate(async () => {
      const res = await fetch('/api/works?type=GAME&size=40');
      const data = await res.json();
      return data.content.flatMap((w: { platforms: string[] }) => w.platforms);
    });

    expect(platforms.length).toBeGreaterThan(0);
    // parent_platforms 라 묶음 이름이어야 한다 — "PlayStation 4" 같은 기종명이면 잘못 담은 것
    expect(platforms).toContain('PC');
    expect(platforms.some((p: string) => /^PlayStation \d/.test(p))).toBe(false);
  });

  test('★애니에는 플랫폼이 없다★', async ({ page }) => {
    await page.goto('/');
    const platforms = await page.evaluate(async () => {
      const res = await fetch('/api/works?type=ANIME&size=20');
      const data = await res.json();
      return data.content.flatMap((w: { platforms: string[] }) => w.platforms);
    });

    expect(platforms).toHaveLength(0);
  });
});

test.describe('플랫폼 — 화면', () => {
  test('게임 상세에 플랫폼 칩이 나온다', async ({ page }) => {
    await page.goto('/');
    const gameId = await findGameWithPlatforms(page);

    await page.goto(`/work/${gameId}`);
    await expect(page.getByText('플랫폼', { exact: true })).toBeVisible();
  });

  test('★애니 상세에는 플랫폼 줄이 아예 없다★', async ({ page }) => {
    await page.goto('/work/1');
    await expect(page.getByRole('heading', { name: /Frieren/ })).toBeVisible();

    await expect(page.getByText('플랫폼', { exact: true })).toHaveCount(0);
  });

  test('게임 카드 부제에 "출시연도 · 플랫폼"이 나온다', async ({ page }) => {
    await page.goto('/?type=GAME');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    // 카드 부제는 noWrap 한 줄이라 플랫폼을 2개까지만 넣는다
    await expect(page.getByText(/\d{4}년 · [A-Za-z]/).first()).toBeVisible();
  });

  test('애니 카드 부제는 분기 그대로다 (플랫폼이 붙지 않는다)', async ({ page }) => {
    await page.goto('/?type=ANIME');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    await expect(page.getByText(/\d{4}년 · [A-Za-z]/)).toHaveCount(0);
  });

  test('★주요 플랫폼이 앞에 온다★ (RAWG의 알파벳순을 그대로 쓰지 않는다)', async ({ page }) => {
    await page.goto('/');
    // PC와 Apple Macintosh를 함께 가진 게임을 찾는다 — 알파벳순이면 Mac이 앞이다
    const found = await page.evaluate(async () => {
      const res = await fetch('/api/works?type=GAME&size=40');
      const data = await res.json();
      const g = data.content.find((w: { platforms: string[] }) =>
        w.platforms.includes('PC') && w.platforms.includes('Apple Macintosh'));
      return g ? { id: g.id as number, title: g.title as string } : null;
    });
    test.skip(!found, 'PC + Apple Macintosh를 함께 가진 게임이 없습니다');

    await page.goto(`/work/${found!.id}`);
    await expect(page.getByText('플랫폼', { exact: true })).toBeVisible();

    const chips = await page.locator('.MuiChip-label').allInnerTexts();
    expect(chips.indexOf('PC')).toBeLessThan(chips.indexOf('Apple Macintosh'));
  });
});
