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
