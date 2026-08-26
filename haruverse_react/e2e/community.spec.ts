import { test, expect, type Page } from '@playwright/test';

function uniqueEmail(): string {
  return `post_${Date.now()}_${Math.floor(Math.random() * 1_000_000)}@haru.test`;
}

const PASSWORD = 'test1234!';

async function signupAndLogin(page: Page, email: string, nickname: string) {
  await page.goto('/signup');
  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '닉네임' }).fill(nickname);
  await page.getByRole('textbox', { name: '비밀번호', exact: true }).fill(PASSWORD);
  await page.getByRole('textbox', { name: '비밀번호 확인' }).fill(PASSWORD);
  await page.getByRole('button', { name: '가입하기' }).click();
  await expect(page).toHaveURL(/\/login$/);
  // URL만 보고 넘어가면 React가 아직 렌더 전이라 fill이 state에 반영되지 않는다
  await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();

  await page.getByRole('textbox', { name: '이메일' }).fill(email);
  await page.getByRole('textbox', { name: '비밀번호' }).fill(PASSWORD);
  await page.locator('form').getByRole('button', { name: '로그인' }).click();
  await expect(page).toHaveURL(/\/$/);
}

/** 작품 1(프리렌) 게시판에 글을 하나 쓰고 그 제목을 돌려준다 */
async function writePost(page: Page, title: string, content: string) {
  await page.goto('/work/1/posts');
  await page.getByRole('button', { name: '글쓰기' }).click();
  await expect(page.getByRole('heading', { name: '글쓰기' })).toBeVisible();

  await page.getByRole('textbox', { name: '제목' }).fill(title);
  await page.getByRole('textbox', { name: '내용' }).fill(content);
  await page.getByRole('button', { name: '등록' }).click();

  // 등록하면 게시판으로 돌아오고, 최신순이라 맨 위에 있다
  await expect(page).toHaveURL(/\/work\/1\/posts$/);
  await expect(page.getByRole('cell', { name: title })).toBeVisible();
}

test.describe('게시판 — 진입', () => {
  test('작품 상세에서 게시판으로 갈 수 있다', async ({ page }) => {
    await page.goto('/work/1');
    await expect(page.getByRole('heading', { name: /Frieren/ })).toBeVisible();

    await page.getByRole('button', { name: '게시판' }).click();

    await expect(page).toHaveURL(/\/work\/1\/posts$/);
    await expect(page.getByRole('heading', { name: /게시판/ })).toBeVisible();
  });

  test('비로그인은 글을 읽을 수 있다', async ({ page }) => {
    await page.goto('/work/1/posts');
    await expect(page.getByRole('heading', { name: /게시판/ })).toBeVisible();
    // 목록 자체가 보이면 통과 (글이 없으면 안내 문구)
    await expect(page.getByRole('columnheader', { name: '제목' })).toBeVisible();
  });

  test('비로그인이 글쓰기를 누르면 로그인 화면으로', async ({ page }) => {
    await page.goto('/work/1/posts');
    await page.getByRole('button', { name: '글쓰기' }).click();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('잘못된 주소 → 안내', async ({ page }) => {
    await page.goto('/work/abc/posts');
    await expect(page.getByText('잘못된 게시판 주소입니다.')).toBeVisible();
  });
});

test.describe('게시판 — 글 작성·조회', () => {
  test('글을 쓰면 목록에 나오고 상세로 들어갈 수 있다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '글쓴이');
    const title = `테스트 글 ${Date.now()}`;
    await writePost(page, title, '본문입니다.\n줄바꿈도 유지됩니다.');

    await page.getByRole('cell', { name: title }).click();
    await expect(page).toHaveURL(/\/post\/\d+$/);
    await expect(page.getByRole('heading', { name: title })).toBeVisible();
    await expect(page.getByText('본문입니다.')).toBeVisible();
  });

  test('제목이나 내용이 비면 안내가 뜬다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '빈값');
    await page.goto('/work/1/posts/new');
    await expect(page.getByRole('heading', { name: '글쓰기' })).toBeVisible();

    await page.getByRole('textbox', { name: '제목' }).fill('제목만 있음');
    await page.getByRole('button', { name: '등록' }).click();

    await expect(page.getByText('제목과 내용을 모두 입력해주세요.')).toBeVisible();
  });

  test('상세를 열면 조회수가 오른다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '조회수');
    const title = `조회수 글 ${Date.now()}`;
    await writePost(page, title, '내용');

    await page.getByRole('cell', { name: title }).click();
    await expect(page.getByText(/조회 \d+/)).toBeVisible();
    const first = await page.getByText(/조회 \d+/).textContent();

    await page.reload();
    await expect(page.getByText(/조회 \d+/)).toBeVisible();
    const second = await page.getByText(/조회 \d+/).textContent();

    expect(parseInt(second!.replace(/\D/g, ''), 10))
      .toBeGreaterThan(parseInt(first!.replace(/\D/g, ''), 10));
  });
});

test.describe('게시판 — 댓글·추천', () => {
  test('댓글을 남기면 목록에 바로 나온다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '댓글러');
    const title = `댓글 글 ${Date.now()}`;
    await writePost(page, title, '내용');
    await page.getByRole('cell', { name: title }).click();

    await page.getByPlaceholder('댓글을 입력하세요').fill('첫 댓글입니다');
    await page.getByRole('button', { name: '등록' }).click();

    await expect(page.getByText('첫 댓글입니다')).toBeVisible();
    await expect(page.getByText('댓글 1')).toBeVisible();
  });

  test('★낙관적 갱신★ 추천을 누르면 숫자가 즉시 오른다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '추천러');
    const title = `추천 글 ${Date.now()}`;
    await writePost(page, title, '내용');
    await page.getByRole('cell', { name: title }).click();

    await expect(page.getByRole('button', { name: '추천 0' })).toBeVisible();
    await page.getByRole('button', { name: '추천 0' }).click();
    await expect(page.getByRole('button', { name: '추천 1' })).toBeVisible();

    // 새로고침해도 유지 — 서버에 저장됐다는 뜻
    await page.reload();
    await expect(page.getByRole('button', { name: '추천 1' })).toBeVisible();
  });

  test('추천을 다시 누르면 취소된다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '취소러');
    const title = `취소 글 ${Date.now()}`;
    await writePost(page, title, '내용');
    await page.getByRole('cell', { name: title }).click();

    await page.getByRole('button', { name: '추천 0' }).click();
    await expect(page.getByRole('button', { name: '추천 1' })).toBeVisible();
    await page.getByRole('button', { name: '추천 1' }).click();
    await expect(page.getByRole('button', { name: '추천 0' })).toBeVisible();
  });
});

test.describe('게시판 — 권한', () => {
  test('내 글에는 수정·삭제 버튼이 보인다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '주인');
    const title = `내 글 ${Date.now()}`;
    await writePost(page, title, '내용');
    await page.getByRole('cell', { name: title }).click();

    await expect(page.getByRole('button', { name: '수정' })).toBeVisible();
    await expect(page.getByRole('button', { name: '삭제', exact: true })).toBeVisible();
  });

  test('★남의 글에는 수정·삭제 버튼이 없다★', async ({ page }) => {
    // ① 한 사람이 글을 쓴다
    await signupAndLogin(page, uniqueEmail(), '작성자');
    const title = `남의 글 ${Date.now()}`;
    await writePost(page, title, '내용');

    // ② 다른 사람으로 로그인해서 같은 글을 본다
    await page.getByRole('button', { name: '계정 메뉴' }).click();
    await page.getByRole('menuitem', { name: '로그아웃' }).click();
    await signupAndLogin(page, uniqueEmail(), '방문자');

    await page.goto('/work/1/posts');
    await page.getByRole('cell', { name: title }).click();

    await expect(page.getByRole('heading', { name: title })).toBeVisible();
    await expect(page.getByRole('button', { name: '수정' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: '삭제', exact: true })).toHaveCount(0);
  });

  test('글을 수정하면 내용이 바뀐다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '수정자');
    const title = `수정 전 ${Date.now()}`;
    await writePost(page, title, '원래 내용');
    await page.getByRole('cell', { name: title }).click();

    await page.getByRole('button', { name: '수정' }).click();
    await expect(page.getByRole('heading', { name: '글 수정' })).toBeVisible();

    const newTitle = `수정 후 ${Date.now()}`;
    await page.getByRole('textbox', { name: '제목' }).fill(newTitle);
    await page.getByRole('button', { name: '수정' }).click();

    await expect(page.getByRole('heading', { name: newTitle })).toBeVisible();
  });

  test('★글을 지우면 댓글도 함께 사라진다★', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '삭제자');
    const title = `지울 글 ${Date.now()}`;
    await writePost(page, title, '내용');
    await page.getByRole('cell', { name: title }).click();

    await page.getByPlaceholder('댓글을 입력하세요').fill('사라질 댓글');
    await page.getByRole('button', { name: '등록' }).click();
    await expect(page.getByText('사라질 댓글')).toBeVisible();

    // window.confirm 자동 수락
    page.on('dialog', (d) => d.accept());
    // ★exact 필수★ 댓글 삭제 버튼(aria-label="댓글 삭제")도 '삭제'를 포함해
    // 부분 일치로는 두 개가 잡힌다
    await page.getByRole('button', { name: '삭제', exact: true }).click();

    // 게시판으로 돌아오고 목록에서 사라진다
    await expect(page).toHaveURL(/\/work\/1\/posts$/);
    await expect(page.getByRole('cell', { name: title })).toHaveCount(0);
  });
});

// 게시판 187개 중 어디에 글이 있는지 알 방법이 없어서 만든 입구
// ★채널(작품) 카드 목록★ — 글이 있는 채널만, 각 카드에 최근 글
test.describe('커뮤니티 입구', () => {
  test('헤더 "커뮤니티" 탭 → /community', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.MuiCardActionArea-root').first()).toBeVisible();

    await expect(page.getByRole('tab', { name: '커뮤니티' })).toBeEnabled();
    await page.getByRole('tab', { name: '커뮤니티' }).click();

    await expect(page).toHaveURL(/\/community/);
    await expect(page.getByRole('heading', { name: '커뮤니티' })).toBeVisible();
  });

  test('★글이 있는 채널만 카드로 나온다★', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '채널테스트');
    const title = `채널 글 ${Date.now()}`;
    await writePost(page, title, '내용');

    await page.goto('/community');
    // 작품 1(프리렌)에 글을 썼으니 그 채널 카드가 있어야 한다
    await expect(page.getByText(/Frieren/).first()).toBeVisible();
    // 작품은 187개지만 카드는 글이 있는 채널만 — 전부 깔리지 않는다
    const cards = await page.locator('.MuiCardActionArea-root').count();
    expect(cards).toBeLessThan(20);
  });

  test('카드에 가장 최근 글이 보인다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '최근글');
    const older = `예전 글 ${Date.now()}`;
    const newer = `최근 글 ${Date.now() + 1}`;
    await writePost(page, older, '내용');
    await writePost(page, newer, '내용');

    await page.goto('/community');
    await expect(page.getByText(newer)).toBeVisible();
    await expect(page.getByText(older)).toHaveCount(0); // 최근 글 하나만
  });

  test('카드에 글 수가 표시된다', async ({ page }) => {
    await page.goto('/community');
    await expect(page.getByText(/^글 \d+개$/).first()).toBeVisible();
  });

  test('★카드를 누르면 그 채널 게시판으로★ (특정 글이 아니라 채널이 단위)', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), '채널이동');
    await writePost(page, `이동 글 ${Date.now()}`, '내용');

    await page.goto('/community');
    await page.locator('.MuiCardActionArea-root').first().click();

    await expect(page).toHaveURL(/\/work\/\d+\/posts$/);
    await expect(page.getByRole('heading', { name: /게시판/ })).toBeVisible();
  });

  test('★검색은 홈으로 보낸다★ (커뮤니티엔 아직 글 검색이 없다)', async ({ page }) => {
    await page.goto('/community');
    await expect(page.getByRole('heading', { name: '커뮤니티' })).toBeVisible();

    await page.getByLabel('작품 검색').fill('Frieren');
    await page.getByLabel('작품 검색').press('Enter');

    await expect(page).not.toHaveURL(/\/community/);
    await expect(page.getByRole('heading', { name: /'Frieren' 검색 결과/ })).toBeVisible();
  });
});


/**
 * 글 상세 사이드바 — 작품 정보 + 같은 채널의 다른 글.
 *
 * 본문만 있으면 글 하나 읽고 나가버린다. 사이드바는 "다음 글로" 이어주는 자리다.
 * 새 API 없이 기존 두 엔드포인트(작품 상세 · 게시판 목록)만 쓴다.
 */
test.describe('글 상세 사이드바', () => {
  /** 글을 하나 쓰고 그 상세로 들어간다 */
  async function writeAndOpen(page: Page, title: string) {
    await writePost(page, title, `${title} 본문`);
    await page.getByRole('cell', { name: title }).click();
    await expect(page).toHaveURL(/\/post\/\d+$/);
  }

  test('작품 정보가 보인다 (분류·찜하기)', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), `side_${Date.now()}`);
    await writeAndOpen(page, `사이드바 작품정보 ${Date.now()}`);

    await expect(page.getByText('이 게시판의 다른 글')).toBeVisible();
    // 작품 카드 — 도감과 같은 찜하기를 사이드바에서도 누를 수 있다
    await expect(page.getByRole('button', { name: /찜하기|찜함/ })).toBeVisible();
    // 장르 칩 — '애니메이션'은 헤더 탭에도 있어서 장르로 확인한다
    await expect(page.getByText('Fantasy', { exact: true })).toBeVisible();
  });

  test('★지금 보고 있는 글은 "다른 글"에 나오지 않는다★', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), `side_${Date.now()}`);
    const title = `사이드바 중복확인 ${Date.now()}`;
    await writeAndOpen(page, title);

    // 제목은 본문 헤더에 한 번만 — 사이드바에 자기 자신이 또 뜨면 2가 된다
    await expect(page.getByText(title, { exact: true })).toHaveCount(1);
  });

  test('다른 글을 누르면 그 글로 이동한다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), `side_${Date.now()}`);
    const first = `사이드바 이전글 ${Date.now()}`;
    await writeAndOpen(page, first);
    const firstUrl = page.url();

    // 같은 채널에 글을 하나 더 — 두 번째 글에서 첫 번째 글이 사이드바에 보인다
    await writeAndOpen(page, `사이드바 다음글 ${Date.now()}`);
    await page.getByRole('button', { name: first }).click();

    await expect(page).toHaveURL(firstUrl);
  });

  test('"게시판 전체 보기"로 목록에 돌아간다', async ({ page }) => {
    await signupAndLogin(page, uniqueEmail(), `side_${Date.now()}`);
    await writeAndOpen(page, `사이드바 목록이동 ${Date.now()}`);

    await page.getByRole('button', { name: '게시판 전체 보기' }).click();
    await expect(page).toHaveURL(/\/work\/1\/posts$/);
  });
});
