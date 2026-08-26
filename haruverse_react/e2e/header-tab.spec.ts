import { test, expect } from '@playwright/test';

/**
 * 헤더 탭 하이라이트 — "지금 어디에 있는지"를 알려주는 유일한 표시다.
 *
 * ★목록과 상세가 단수/복수로 갈린다★ (/characters ↔ /character/1)
 * 예전 로직은 `pathname.startsWith(path)` 라 상세를 하나도 못 잡았고,
 * 못 잡으면 `Math.max(0, ...)` 가 0번을 골라 **어디서든 '전체'** 가 켜져 있었다.
 * 라우트를 새로 추가할 때 Layout 의 CATEGORIES.match 갱신을 잊으면 여기서 걸린다.
 */
const CASES: Array<[string, string | null]> = [
  // 홈 — type 쿼리로 갈린다
  ['/', '전체'],
  ['/?type=ANIME', '애니메이션'],
  ['/?type=GAME', '게임'],

  // 별도 도감 — 목록과 상세가 같은 탭이어야 한다
  ['/characters', '캐릭터'],
  ['/character/1', '캐릭터'],
  ['/voice-actors', '성우'],
  ['/voice-actor/1', '성우'],
  ['/studios', '제작사'],

  // 커뮤니티는 경로가 세 갈래
  ['/community', '커뮤니티'],
  ['/work/1/posts', '커뮤니티'],
  ['/post/1', '커뮤니티'],

  // ★어느 탭도 아닌 화면★ — 아무것도 켜지지 않아야 한다
  ['/work/1', null],   // 작품 도감. 애니인지 게임인지는 헤더가 알 수 없다
  ['/about', null],
  ['/login', null],
  ['/mypage', null],
];

for (const [route, expected] of CASES) {
  test(`${route} → ${expected ?? '선택 없음'}`, async ({ page }) => {
    await page.goto(route);
    const selected = page.locator('button[role="tab"][aria-selected="true"]');

    if (expected === null) {
      await expect(selected).toHaveCount(0);
    } else {
      await expect(selected).toHaveText(expected);
    }
  });
}

test('★상세에서 검색하면 목록으로 간다★ (상세 경로에 q를 붙이지 않는다)', async ({ page }) => {
  await page.goto('/character/1');
  await page.getByPlaceholder(/검색/).fill('frieren');
  await page.getByPlaceholder(/검색/).press('Enter');

  await expect(page).toHaveURL(/\/characters\?q=frieren$/);
});

test('커뮤니티에서 검색하면 홈으로 간다 (글 검색 API가 아직 없다)', async ({ page }) => {
  await page.goto('/post/1');
  await page.getByPlaceholder(/검색/).fill('frieren');
  await page.getByPlaceholder(/검색/).press('Enter');

  await expect(page).toHaveURL(/\/\?q=frieren$/);
});
