import { test, expect, type Page } from '@playwright/test';

/**
 * Elasticsearch 통합검색.
 *
 * <p>여기 있는 테스트는 <b>ES가 떠 있어야</b> 의미가 있다. DB의 LIKE 검색으로는
 * 통과할 수 없는 것들만 골라 담았다 — 오타 교정과 관련도 정렬.
 * ES가 없으면 앱은 DB 검색으로 폴백하므로 <b>실패가 아니라 skip</b> 한다.
 * (검색이 부가 기능이라는 설계를 테스트도 그대로 따른다)
 */

/** 검색 결과 제목들 */
async function search(page: Page, query: string): Promise<string[]> {
  return page.evaluate(async (q) => {
    const res = await fetch(`/api/works?q=${encodeURIComponent(q)}&size=20`);
    const data = await res.json();
    return data.content.map((w: { title: string }) => w.title);
  }, query);
}

/** ES가 살아 있는지 — 오타 검색이 되면 ES다 (LIKE로는 절대 안 나온다) */
async function elasticsearchAlive(page: Page): Promise<boolean> {
  return (await search(page, 'frieran')).length > 0;
}

test.describe('통합검색 — Elasticsearch', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('★오타를 교정한다★ frieran → Frieren (LIKE로는 0건)', async ({ page }) => {
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    const titles = await search(page, 'frieran');

    expect(titles.length).toBeGreaterThan(0);
    expect(titles.every((t) => /frieren/i.test(t))).toBe(true);
  });

  test('★관련도 순으로 나온다★ (최신순이 아니다)', async ({ page }) => {
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    const titles = await search(page, 'frieren');

    // 제목이 정확히 일치하는 본편이, 더 최근에 나온 Season 2보다 위여야 한다.
    // 컨트롤러에 기본 정렬(releaseDate DESC)이 남아 있으면 이게 뒤집힌다 —
    // 실제로 그 버그를 이 검증으로 잡았다.
    expect(titles[0]).toBe("Frieren: Beyond Journey's End");
  });

  test('소유격이 분리된다 (journey → Journey\'s)', async ({ page }) => {
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    const titles = await search(page, 'journey');

    // standard 토크나이저는 "Journey's"를 한 덩어리로 둔다.
    // possessive_english 스테머가 없으면 이 검색이 0건이 된다.
    expect(titles.some((t) => /Journey's/i.test(t))).toBe(true);
  });

  test('★오타 허용이 줄거리까지 번지지 않는다★', async ({ page }) => {
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    const titles = await search(page, 'elden rong');

    // 줄거리에도 fuzziness를 걸었더니 무관한 애니 29편이 나왔다.
    // 제목 계열에만 오타를 허용하도록 고친 뒤의 기대: Elden Ring이 1위, 결과는 소수
    expect(titles[0]).toBe('Elden Ring');
    expect(titles.length).toBeLessThan(10);
  });

  test('검색과 종류 필터가 함께 걸린다', async ({ page }) => {
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    const games = await page.evaluate(async () => {
      const res = await fetch('/api/works?q=zelda&type=GAME&size=20');
      return (await res.json()).totalElements as number;
    });
    const anime = await page.evaluate(async () => {
      const res = await fetch('/api/works?q=zelda&type=ANIME&size=20');
      return (await res.json()).totalElements as number;
    });

    expect(games).toBeGreaterThan(0);
    expect(anime).toBe(0); // 젤다는 게임이다 — 필터가 무시되면 여기서 걸린다
  });
});

test.describe('통합검색 — 폴백', () => {
  test('검색어가 없으면 최신순 목록이다 (ES를 타지 않는다)', async ({ page }) => {
    await page.goto('/');

    const years = await page.evaluate(async () => {
      const res = await fetch('/api/works?type=ANIME&size=5');
      const data = await res.json();
      return data.content.map((w: { releaseDate: string | null }) => w.releaseDate);
    });

    // 내림차순이어야 한다 — 검색이 아닐 때의 기본 정렬은 서비스가 붙인다
    const dates = years.filter(Boolean) as string[];
    expect(dates).toEqual([...dates].sort().reverse());
  });

  test('검색 화면은 ES 없이도 열린다 (부가 기능이라는 설계)', async ({ page }) => {
    await page.goto('/?q=frieren');

    // ES가 죽어 있어도 DB 검색으로 결과가 나와야 한다
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();
  });
});

/**
 * 자동완성 — 검색창에 앞글자만 쳐도 후보가 뜬다.
 *
 * <p>edge_ngram 색인이 있어야 동작한다. ES가 없으면 후보가 안 뜨는 게 정상이므로
 * (폴백을 일부러 두지 않았다) 그 경우엔 skip 한다.
 */
test.describe('자동완성', () => {
  const SEARCH = '작품 검색…';

  async function type(page: Page, text: string) {
    const box = page.getByPlaceholder(SEARCH);
    await box.click();
    await box.fill(text);
    await page.waitForTimeout(700); // 디바운스 250ms + 응답
  }

  test('앞글자만 쳐도 후보가 뜬다 (fri → Frieren)', async ({ page }) => {
    await page.goto('/');
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    await type(page, 'fri');

    const titles = await page.locator('li[role="menuitem"]').allInnerTexts();
    expect(titles.length).toBeGreaterThan(0);
    expect(titles.every((t) => /frieren/i.test(t))).toBe(true);
  });

  test('★한 글자에는 반응하지 않는다★ (거의 전부가 뜬다)', async ({ page }) => {
    await page.goto('/');
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    await type(page, 'f');

    await expect(page.locator('li[role="menuitem"]')).toHaveCount(0);
  });

  test('후보를 누르면 그 작품 상세로 간다 (검색 결과를 거치지 않는다)', async ({ page }) => {
    await page.goto('/');
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    await type(page, 'elden');
    await page.locator('li[role="menuitem"]').first().click();

    await expect(page).toHaveURL(/\/work\/\d+$/);
  });

  test('키보드로 고를 수 있다 (↓ + Enter)', async ({ page }) => {
    await page.goto('/');
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    await type(page, 'zelda');
    await page.getByPlaceholder(SEARCH).press('ArrowDown');
    await page.getByPlaceholder(SEARCH).press('Enter');

    await expect(page).toHaveURL(/\/work\/\d+$/);
  });

  test('Esc를 누르면 후보가 닫힌다', async ({ page }) => {
    await page.goto('/');
    test.skip(!(await elasticsearchAlive(page)), 'Elasticsearch가 떠 있지 않습니다');

    await type(page, 'fri');
    await expect(page.locator('li[role="menuitem"]').first()).toBeVisible();

    await page.getByPlaceholder(SEARCH).press('Escape');
    await expect(page.locator('li[role="menuitem"]')).toHaveCount(0);
  });

  test('후보를 고르지 않고 Enter를 치면 평소대로 검색된다', async ({ page }) => {
    await page.goto('/');

    const box = page.getByPlaceholder(SEARCH);
    await box.click();
    await box.fill('frieren');
    await box.press('Enter');

    await expect(page).toHaveURL(/\/\?q=frieren$/);
  });
});

/**
 * 한글 검색 — TMDB 에서 가져온 한국어 제목으로 찾는다.
 *
 * <p>오랫동안 안 됐던 기능이다. Jikan·RAWG 가 주는 제목이 전부 영문이라
 * <b>검색 엔진 문제가 아니라 데이터 문제</b>였다.
 * TMDB 로 titleKo 를 채우고, ES 색인에 이미 열어둔 자리에 넣어 해결했다.
 *
 * <p>수집이 안 돼 있으면(키 없음·미수집) 후보가 0건이므로 skip 한다.
 */
test.describe('한글 검색', () => {
  async function koreanTitlesExist(page: Page): Promise<boolean> {
    return (await search(page, '프리렌')).length > 0;
  }

  test('★"프리렌"으로 Frieren 을 찾는다★', async ({ page }) => {
    await page.goto('/');
    test.skip(!(await koreanTitlesExist(page)), '한국어 제목이 수집되지 않았습니다');

    const titles = await search(page, '프리렌');

    expect(titles.length).toBeGreaterThan(0);
    expect(titles.every((t) => /frieren/i.test(t))).toBe(true);
  });

  test('다른 작품도 한글로 찾힌다', async ({ page }) => {
    await page.goto('/');
    test.skip(!(await koreanTitlesExist(page)), '한국어 제목이 수집되지 않았습니다');

    expect(await search(page, '귀멸의 칼날')).toContain('Demon Slayer: Kimetsu no Yaiba');
    expect(await search(page, '카우보이 비밥')).toContain('Cowboy Bebop');
  });

  test('한글 자동완성도 동작한다', async ({ page }) => {
    await page.goto('/');
    test.skip(!(await koreanTitlesExist(page)), '한국어 제목이 수집되지 않았습니다');

    const titles = await page.evaluate(async () => {
      const res = await fetch(`/api/works/suggest?q=${encodeURIComponent('프리')}`);
      return (await res.json()).map((w: { title: string }) => w.title);
    });

    expect(titles.length).toBeGreaterThan(0);
    expect(titles.every((t: string) => /frieren/i.test(t))).toBe(true);
  });

  test('★한국어 제목이 없는 작품은 영문으로만 찾힌다★ (억지로 채우지 않았다)', async ({ page }) => {
    await page.goto('/');

    // 게임은 TMDB 에 없어서 titleKo 가 비어 있다. 그래도 영문 검색은 된다.
    const games = await page.evaluate(async () => {
      const res = await fetch('/api/works?q=elden&type=GAME&size=5');
      return (await res.json()).totalElements as number;
    });

    expect(games).toBeGreaterThan(0);
  });
});
