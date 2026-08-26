import { test, expect, type Page } from '@playwright/test';

/** 캐릭터 그리드가 실제로 그려질 때까지 기다린다 */
async function waitForCharacters(page: Page) {
  await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible({ timeout: 15000 });
}

test.describe('캐릭터 도감 — 목록', () => {
  test('헤더 "캐릭터" 탭 → /characters 로 이동한다', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    // 예전에는 비활성이었다 — 이제 눌려야 한다
    await expect(page.getByRole('tab', { name: '캐릭터' })).toBeEnabled();
    await page.getByRole('tab', { name: '캐릭터' }).click();

    await expect(page).toHaveURL(/\/characters/);
    await expect(page.getByRole('heading', { name: '캐릭터 도감' })).toBeVisible();
    await waitForCharacters(page);
  });

  test('캐릭터 탭에 있으면 탭이 활성 상태로 남는다', async ({ page }) => {
    await page.goto('/characters');
    await expect(page.getByRole('tab', { name: '캐릭터' })).toHaveAttribute('aria-selected', 'true');
  });

  test('인기순으로 정렬된다 (즐겨찾기 수 내림차순)', async ({ page }) => {
    await page.goto('/characters');
    await waitForCharacters(page);

    // 카드의 인기 뱃지(32.4K 같은 형식)를 순서대로 읽어 내림차순인지 본다
    const badges = await page.locator('.MuiChip-root').filter({ hasText: /^[\d.]+[KM]?$/ }).allTextContents();
    const nums = badges.map((t) => {
      const n = parseFloat(t);
      if (t.endsWith('M')) return n * 1_000_000;
      if (t.endsWith('K')) return n * 1_000;
      return n;
    });
    expect(nums.length).toBeGreaterThan(1);
    for (let i = 1; i < nums.length; i++) {
      expect(nums[i]).toBeLessThanOrEqual(nums[i - 1]);
    }
  });

  test('헤더 검색이 캐릭터 안에서 동작한다 (홈으로 튕기지 않는다)', async ({ page }) => {
    await page.goto('/characters');
    await waitForCharacters(page);

    await page.getByLabel('작품 검색').fill('Frieren');
    await page.getByLabel('작품 검색').press('Enter');

    await expect(page).toHaveURL(/\/characters\?.*q=Frieren/);
    await expect(page.getByRole('heading', { name: /'Frieren' 캐릭터 검색 결과/ })).toBeVisible();
    await expect(page.locator('.MuiCardActionArea-root')).toHaveCount(1);
  });

  test('검색 결과 없음 → 안내 문구', async ({ page }) => {
    await page.goto('/characters?q=존재하지않는캐릭터xyz');
    await expect(page.getByText('조건에 맞는 캐릭터가 없어요.')).toBeVisible();
  });
});

test.describe('캐릭터 도감 — 상세', () => {
  test('카드를 누르면 상세로 가고 출연 작품이 나온다', async ({ page }) => {
    await page.goto('/characters?q=Frieren');
    await waitForCharacters(page);

    await page.locator('.MuiCardActionArea-root').first().click();

    await expect(page).toHaveURL(/\/character\/\d+/);
    await expect(page.getByRole('heading', { name: 'Frieren' })).toBeVisible();
    // 성우와 출연작
    await expect(page.getByText('Tanezaki, Atsumi')).toBeVisible();
    await expect(page.getByText(/출연 작품 \d+편/)).toBeVisible();
  });

  test('출연 작품을 누르면 그 작품 상세로 간다', async ({ page }) => {
    await page.goto('/characters?q=Frieren');
    await waitForCharacters(page);
    await page.locator('.MuiCardActionArea-root').first().click();
    await expect(page.getByRole('heading', { name: 'Frieren' })).toBeVisible();

    // 출연작 카드(캐릭터 카드가 아니라 목록형 카드)
    await page.locator('.MuiCardActionArea-root').first().click();
    await expect(page).toHaveURL(/\/work\/\d+/);
  });

  test('잘못된 주소 → 안내', async ({ page }) => {
    await page.goto('/character/abc');
    await expect(page.getByText('잘못된 캐릭터 주소입니다.')).toBeVisible();
  });
});

test.describe('작품 상세 — 등장인물 섹션', () => {
  test('애니 상세에 등장인물이 나온다 (주역이 앞에)', async ({ page }) => {
    await page.goto('/work/1');
    await expect(page.getByRole('heading', { name: /Frieren/ })).toBeVisible();

    await expect(page.getByText(/등장인물\s*\d+명/)).toBeVisible();
    // 첫 캐릭터에 '주역' 뱃지
    await expect(page.locator('.MuiChip-root', { hasText: '주역' }).first()).toBeVisible();
  });

  test('인원이 많으면 일부만 보이고 "더 보기"가 있다', async ({ page }) => {
    await page.goto('/work/1');
    await expect(page.getByText(/등장인물\s*\d+명/)).toBeVisible();

    const more = page.getByRole('button', { name: /나머지 \d+명 더 보기/ });
    await expect(more).toBeVisible();

    const before = await page.locator('.MuiCardActionArea-root').count();
    await more.click();
    await expect(more).toHaveCount(0); // 버튼이 사라진다
    expect(await page.locator('.MuiCardActionArea-root').count()).toBeGreaterThan(before);
  });

  test('등장인물 카드를 누르면 캐릭터 상세로 간다', async ({ page }) => {
    await page.goto('/work/1');
    await expect(page.getByText(/등장인물\s*\d+명/)).toBeVisible();

    // 등장인물 섹션의 첫 카드
    await page.locator('.MuiCardActionArea-root').first().click();
    await expect(page).toHaveURL(/\/character\/\d+/);
  });

  test('캐릭터가 없는 작품에는 섹션이 아예 없다', async ({ page }) => {
    // ★특정 게임을 이름으로 지정한다★
    //   전에는 게임 목록의 '첫 카드'를 눌렀는데, 붕괴: 스타레일처럼
    //   캐릭터가 있는 게임이 앞에 오면 테스트가 깨진다(출시일 순이라 순서가 바뀐다).
    await page.goto('/?q=Baldur');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();
    await page.locator('.MuiCardActionArea-root').first().click();
    await expect(page).toHaveURL(/\/work\/\d+/);
    await expect(page.getByText('줄거리')).toBeVisible();

    // 빈 섹션 제목조차 나오면 안 된다
    await expect(page.getByText(/등장인물/)).toHaveCount(0);
  });
});

// 캐릭터 출처가 Jikan 하나가 아니게 되면서 생긴 검증
test.describe('게임 캐릭터 (붕괴: 스타레일)', () => {
  test('게임에도 등장인물이 나온다', async ({ page }) => {
    await page.goto('/?q=Honkai');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();
    await page.locator('.MuiCardActionArea-root').first().click();

    await expect(page.getByRole('heading', { name: /Honkai/ })).toBeVisible();
    await expect(page.getByText(/등장인물\s*\d+명/)).toBeVisible();
  });

  test('스타레일 캐릭터가 캐릭터 도감에서 검색된다', async ({ page }) => {
    await page.goto('/characters?q=March 7th');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    await page.locator('.MuiCardActionArea-root').first().click();
    await expect(page.getByRole('heading', { name: 'March 7th' })).toBeVisible();
    // 출연 작품이 붕괴: 스타레일
    await expect(page.getByText(/Honkai/)).toBeVisible();
  });

  test('MAL에 없는 캐릭터라 성우 칩이 없다', async ({ page }) => {
    await page.goto('/characters?q=Kafka');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();
    await page.locator('.MuiCardActionArea-root').first().click();

    await expect(page.getByRole('heading', { name: 'Kafka' })).toBeVisible();
    // 이 출처에는 성우 정보가 없다
    await expect(page.locator('.MuiChip-root').filter({ hasText: /,\s/ })).toHaveCount(0);
  });
});
