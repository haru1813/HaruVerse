import { test, expect } from '@playwright/test';

// 헤더 검색바 · 카테고리 탭 · 페이지네이션이 URL 쿼리스트링과 연동되는지 검증.
// (상태를 URL에 두었으므로 새로고침·뒤로가기까지 함께 확인한다)
test.describe('검색 · 카테고리 필터', () => {
  test('검색어 입력 후 Enter → URL에 q가 실리고 결과가 걸러진다', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    await page.getByLabel('작품 검색').fill('Bleach');
    await page.getByLabel('작품 검색').press('Enter');

    await expect(page).toHaveURL(/[?&]q=Bleach/);
    await expect(page.getByRole('heading', { name: /'Bleach' 검색 결과/ })).toBeVisible();

    // 결과 카드 제목에 검색어가 들어있어야 함
    const first = page.locator('.MuiCardActionArea-root').first();
    await expect(first).toContainText(/Bleach/i);
  });

  test('검색 결과 없음 → 안내 문구', async ({ page }) => {
    await page.goto('/?q=존재하지않는작품명xyz');

    await expect(page.getByText('조건에 맞는 작품이 없어요.')).toBeVisible();
    await expect(page.getByText('검색어나 카테고리를 바꿔보세요.')).toBeVisible();
  });

  test('카테고리 탭(게임) → type=GAME 으로 필터', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('tab', { name: '게임' }).click();

    await expect(page).toHaveURL(/[?&]type=GAME/);
    // 히어로 배너 부제에도 '게임'이 있어 exact가 필요하다
    await expect(page.getByRole('heading', { name: '게임', exact: true })).toBeVisible();
    // RAWG 수집 후에는 결과가 나온다
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();
  });

  test('카테고리 탭(애니메이션) → 결과가 나온다', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('tab', { name: '애니메이션' }).click();

    await expect(page).toHaveURL(/[?&]type=ANIME/);
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();
  });

  test('구현이 끝난 탭은 전부 활성이다', async ({ page }) => {
    await page.goto('/');
    // 미구현 탭(순위)은 제거했다 — 지금은 비활성 탭이 하나도 없어야 한다
    await expect(page.getByRole('tab', { disabled: true })).toHaveCount(0);
    await expect(page.getByRole('tab', { name: '캐릭터' })).toBeEnabled();
    await expect(page.getByRole('tab', { name: '제작사' })).toBeEnabled();
  });

  test('필터 칩의 X로 조건을 해제한다', async ({ page }) => {
    await page.goto('/?type=ANIME&q=Bleach');

    await expect(page.getByText('검색: Bleach')).toBeVisible();
    // 검색 칩 삭제 → q만 빠지고 type은 유지
    await page.locator('.MuiChip-root', { hasText: '검색: Bleach' }).getByTestId('CancelIcon').click();

    await expect(page).toHaveURL(/[?&]type=ANIME/);
    await expect(page).not.toHaveURL(/[?&]q=/);
  });

  test('검색 상태가 새로고침 후에도 유지된다 (URL이 진실의 원천)', async ({ page }) => {
    await page.goto('/?q=Bleach');
    await expect(page.getByLabel('작품 검색')).toHaveValue('Bleach');

    await page.reload();
    await expect(page.getByLabel('작품 검색')).toHaveValue('Bleach');
    await expect(page.getByRole('heading', { name: /'Bleach' 검색 결과/ })).toBeVisible();
  });

  test('뒤로가기로 이전 검색 상태로 돌아간다', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: '작품 도감' })).toBeVisible();

    await page.getByLabel('작품 검색').fill('Bleach');
    await page.getByLabel('작품 검색').press('Enter');
    await expect(page).toHaveURL(/[?&]q=Bleach/);
    // ★URL만 보고 goBack 하면 안 된다★
    //   navigate() 직후 URL은 이미 바뀌었지만 React는 아직 렌더를 커밋하지 않았을 수 있다.
    //   그 상태에서 뒤로가기를 하면 라우터가 중간 상태에서 꼬인다.
    //   → 화면이 실제로 바뀐 것을 확인한 뒤 이동한다.
    await expect(page.getByRole('heading', { name: /'Bleach' 검색 결과/ })).toBeVisible();

    await page.goBack();
    await expect(page).not.toHaveURL(/[?&]q=/);
    await expect(page.getByRole('heading', { name: '작품 도감' })).toBeVisible();
    await expect(page.getByLabel('작품 검색')).toHaveValue(''); // 입력창도 함께 복원
  });

  test('페이지네이션 → URL에 page가 실린다', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    await page.getByRole('button', { name: 'Go to page 2' }).click();
    await expect(page).toHaveURL(/[?&]page=2/);
  });
});

// 장르 — 카드/상세의 장르 칩 → ?genre= 필터
test.describe('장르 필터', () => {
  test('카드의 장르 칩 클릭 → 그 장르로 필터링', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    // 첫 카드의 첫 장르 칩
    const chip = page.locator('.MuiChip-root').first();
    const genre = (await chip.textContent())!.trim();
    await chip.click();

    await expect(page).toHaveURL(new RegExp(`genre=${encodeURIComponent(genre)}`));
    await expect(page.getByRole('heading', { name: `${genre} 장르` })).toBeVisible();
  });

  test('장르 칩 클릭이 상세 이동으로 새지 않는다', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    await page.locator('.MuiChip-root').first().click();
    // /work/:id 로 가면 안 됨 (칩이 CardActionArea 밖에 있어야 통과)
    await expect(page).not.toHaveURL(/\/work\/\d+/);
  });

  test('상세 페이지에 제작사·장르가 표시된다', async ({ page }) => {
    await page.goto('/work/1');
    await expect(page.getByRole('heading', { name: /Frieren/ })).toBeVisible();
    // '제작사'는 헤더의 비활성 탭에도 있으므로 본문 텍스트 전체로 특정한다
    await expect(page.getByText('제작사 Madhouse')).toBeVisible();
    await expect(page.locator('.MuiChip-root', { hasText: 'Fantasy' })).toBeVisible();
  });

  test('장르 필터 결과의 모든 카드가 해당 장르를 가진다', async ({ page }) => {
    await page.goto('/?genre=Fantasy');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    const count = await page.locator('.MuiCardActionArea-root').count();
    expect(count).toBeGreaterThan(0);
    await expect(page.getByRole('heading', { name: 'Fantasy 장르' })).toBeVisible();
  });
});
