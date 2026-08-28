import { useState } from "react";
import type { ReactNode } from "react";
import { useNavigate, useSearchParams, useLocation, Link as RouterLink } from "react-router-dom";
import {
  AppBar, Toolbar, Typography, Box, InputBase, Button, Container, Tabs, Tab,
  Stack, Link, IconButton, Divider, Avatar, Menu, MenuItem, ListItemIcon,
  Popper, Paper, ClickAwayListener, MenuList,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import GitHubIcon from "@mui/icons-material/GitHub";
import PersonOutlineIcon from "@mui/icons-material/PersonOutlined";
import LogoutIcon from "@mui/icons-material/Logout";
import CloseIcon from "@mui/icons-material/Close";
import { useSuggestions } from "../hooks/useSuggestions";
import { useAuth } from "../contexts/AuthContext";
import { SITE } from "../lib/site";

/** 푸터 링크 한 항목 */
type FooterLink = {
  label: string;
  /** 앱 내부 경로 (React Router로 이동) */
  to?: string;
  /** 외부 URL (새 탭으로 열림) */
  href?: string;
  /** false면 아직 만들지 않은 메뉴 — 눌리지 않게 한다 */
  ready?: boolean;
};

const footerLinkSx = {
  color: "rgba(255,255,255,0.7)",
  fontSize: 14,
  width: "fit-content",
  "&:hover": { color: "#fff" },
} as const;

/**
 * 푸터 링크 컬럼 — 제목 + 링크 목록.
 *
 * <p>★내부 링크에 &lt;Link href&gt;를 쓰면 안 된다★
 * MUI Link는 결국 &lt;a href&gt;라서, 브라우저가 문서를 통째로 다시 받아온다.
 * (화면이 하얘졌다가 돌아오고, 로그인 상태·스크롤 위치가 초기화된다)
 * → component={RouterLink} to=... 로 위임해야 SPA 이동이 된다.
 *
 * <p>외부 링크는 반대로 진짜 href가 맞고, target="_blank"에는
 * rel="noopener noreferrer"를 붙인다 (새 창이 window.opener로 원래 탭을 조작하는 것을 막는다).
 */
function FooterLinkColumn({
  title,
  items,
  extra,
}: {
  title: string;
  items: FooterLink[];
  /** 링크 목록 아래에 덧붙일 요소 (GitHub 버튼 등) */
  extra?: ReactNode;
}) {
  return (
    <Box>
      <Typography
        sx={{ fontWeight: 700, fontSize: 13, letterSpacing: "0.06em", color: "#38bdf8", mb: 1.5, textTransform: "uppercase" }}
      >
        {title}
      </Typography>
      <Stack spacing={1.2}>
        {items.map((item) => {
          // 아직 없는 메뉴 — 헤더 탭과 같은 이유로 눌리지 않게 둔다.
          // (동작하지 않는 링크를 눌리게 두면 "고장난 사이트"로 보인다)
          if (item.ready === false) {
            return (
              <Typography
                key={item.label}
                sx={{ color: "rgba(255,255,255,0.3)", fontSize: 14, cursor: "default", width: "fit-content" }}
              >
                {item.label}
                <Box component="span" sx={{ fontSize: 11, ml: 0.8, opacity: 0.8 }}>
                  준비 중
                </Box>
              </Typography>
            );
          }

          if (item.href) {
            return (
              <Link
                key={item.label}
                href={item.href}
                target="_blank"
                rel="noopener noreferrer"
                underline="none"
                sx={footerLinkSx}
              >
                {item.label}
              </Link>
            );
          }

          return (
            <Link
              key={item.label}
              component={RouterLink}
              to={item.to!}
              underline="none"
              // 푸터는 페이지 맨 아래라, 같은 경로에서 쿼리만 바뀌는 이동
              // (예: / → /?type=GAME)에서는 ScrollToTop이 반응하지 않는다.
              onClick={() => window.scrollTo({ top: 0 })}
              sx={footerLinkSx}
            >
              {item.label}
            </Link>
          );
        })}
      </Stack>
      {extra}
    </Box>
  );
}

/**
 * 상단 카테고리 탭.
 *
 * <p>type이 있는 항목은 `/?type=...` 로 필터링되고,
 * ready:false 는 아직 백엔드에 데이터가 없어 비활성으로 둔다.
 * (동작하지 않는 메뉴를 눌리게 두면 "고장난 사이트"로 보인다)
 */
type Category = {
  label: string;
  /** 홈에서 거를 type. match가 없는(=홈에 속한) 항목만 쓴다 */
  type?: string;
  /** 탭을 눌렀을 때 갈 곳. 검색도 이 경로로 보낸다(상세가 아니라 목록으로) */
  path: string;
  ready: boolean;
  /** 그 화면에 검색이 없으면 false — 헤더 검색이 홈으로 떨어진다 */
  searchable?: boolean;
  /**
   * 이 탭이 활성이어야 하는 경로들.
   *
   * ★목록과 상세가 단수/복수로 갈린다★ (/characters ↔ /character/1)
   * 그래서 `pathname.startsWith(path)` 로는 상세를 절대 못 잡는다 —
   * 실제로 /post/30 · /character/1 이 전부 '전체' 탭으로 표시되던 버그의 원인이었다.
   * 규칙을 코드가 아니라 여기 데이터로 적어둔다.
   */
  match?: RegExp[];
};

const CATEGORIES: Category[] = [
  // match가 없는 항목 = 홈(/)에서 type 쿼리로 거르는 탭.
  // 캐릭터처럼 별도 도메인은 자기 경로와 match를 가진다.
  { label: "전체", path: "/", ready: true },
  { label: "애니메이션", type: "ANIME", path: "/", ready: true },
  { label: "게임", type: "GAME", path: "/", ready: true },
  { label: "캐릭터", path: "/characters", ready: true,
    match: [/^\/characters(\/|$)/, /^\/character\/\d+/] },
  { label: "성우", path: "/voice-actors", ready: true,
    match: [/^\/voice-actors(\/|$)/, /^\/voice-actor\/\d+/] },
  // 커뮤니티는 경로가 세 갈래다: 채널 목록 · 작품 게시판 · 글 상세
  // (/work/1 은 작품 도감이라 잡지 않는다 — /work/1/posts 만 커뮤니티)
  { label: "커뮤니티", path: "/community", ready: true, searchable: false,
    match: [/^\/community(\/|$)/, /^\/post\/\d+/, /^\/work\/\d+\/posts/] },
  { label: "제작사", path: "/studios", ready: true, match: [/^\/studios(\/|$)/] },
];

/** 지금 경로가 이 탭에 속하는가 */
function matchesPath(c: Category, pathname: string): boolean {
  return c.match?.some((re) => re.test(pathname)) ?? false;
}

// 공통 레이아웃 셸 — 상단 헤더(로고·검색·로그인) + 카테고리 탭 + 본문 영역.
// children 자리에 각 페이지 내용이 들어온다.
// (나중에 라우터를 붙이면 children 대신 <Outlet />으로 바꾸면 됨)
type LayoutProps = { children: ReactNode };

function Layout({ children }: LayoutProps) {
  const navigate = useNavigate(); // 화면 이동용

  /**
   * 검색어·카테고리의 '진실의 원천'은 컴포넌트 상태가 아니라 ★URL 쿼리스트링★이다.
   *   /?type=ANIME&q=frieren
   *
   * 이렇게 두면 공짜로 얻는 것들:
   *   · 새로고침해도 검색·필터가 유지된다
   *   · 브라우저 뒤로가기가 이전 검색으로 돌아간다
   *   · 링크를 복사해 공유하면 같은 화면이 열린다
   */
  const [searchParams] = useSearchParams();
  const { pathname } = useLocation();
  const currentType = searchParams.get("type") ?? undefined;
  const currentKeyword = searchParams.get("q") ?? "";

  /**
   * 현재 위치로 활성 탭을 역산.
   *
   * 경로가 먼저다 — /characters 에서는 type 쿼리와 무관하게 '캐릭터'가 활성이어야 한다.
   *
   * ★어느 탭도 아니면 false★ (로그인·마이페이지·약관·작품 상세…)
   * 예전엔 Math.max(0, ...) 로 0번을 골라서, 해당 없는 화면마다
   * '전체'에 파란 밑줄이 켜져 있었다. MUI Tabs 는 value={false} 를 주면 아무것도 선택하지 않는다.
   */
  const tab: number | false = (() => {
    const byPath = CATEGORIES.findIndex((c) => matchesPath(c, pathname));
    if (byPath >= 0) return byPath;

    // 홈일 때만 type 쿼리로 고른다.
    // (/about 에서 type이 없다고 '전체'를 켜면 안 된다)
    if (pathname === "/") {
      const byType = CATEGORIES.findIndex((c) => !c.match && c.type === currentType && c.ready);
      if (byType >= 0) return byType;
    }
    return false;
  })();

  // 입력창은 타이핑 중 값을 들고 있어야 하므로 로컬 상태를 둔다.
  // 단, URL이 바뀌면(뒤로가기·링크 진입) 입력창도 따라가야 한다.
  //
  // ★useEffect로 setState 하지 않는 이유★
  //   effect는 화면이 그려진 '뒤에' 돌기 때문에, 옛 값으로 한 번 그린 다음
  //   새 값으로 다시 그리게 된다(깜빡임 + 렌더 2회).
  //   아래처럼 렌더 도중에 조정하면 React가 DOM을 건드리기 전에 즉시 다시 렌더한다.
  //   (React 공식 문서의 "props가 바뀔 때 state 조정하기" 패턴)
  const [keyword, setKeyword] = useState(currentKeyword);

  // ── 자동완성 ──
  // 검색창을 기준점(anchor)으로 후보 목록을 아래에 띄운다.
  // ★ref 가 아니라 state★ — Popper 의 anchorEl 은 렌더 중에 읽히는 값인데,
  //   ref.current 를 렌더에서 읽으면 요소가 붙는 시점에 다시 그려지지 않는다.
  //   콜백 ref 로 state 에 담으면 요소가 생길 때 리렌더가 일어나 위치가 잡힌다.
  const [searchBoxEl, setSearchBoxEl] = useState<HTMLDivElement | null>(null);
  const [suggestOpen, setSuggestOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(-1); // 키보드로 고른 항목 (-1 = 없음)
  const { items: suggestions, clear: clearSuggestions } = useSuggestions(keyword, suggestOpen);

  /** 후보를 고르면 그 작품으로 바로 간다 (검색 결과 목록을 거치지 않는다) */
  const chooseSuggestion = (id: number) => {
    setSuggestOpen(false);
    clearSuggestions();
    setHighlighted(-1);
    navigate(`/work/${id}`);
  };
  const [syncedKeyword, setSyncedKeyword] = useState(currentKeyword);
  if (currentKeyword !== syncedKeyword) {
    setSyncedKeyword(currentKeyword);
    setKeyword(currentKeyword);
  }

  /** 검색 실행 — 홈으로 이동하며 q를 싣는다 (빈 검색어면 q를 지움) */
  const submitSearch = () => {
    const params = new URLSearchParams();
    if (keyword.trim()) params.set("q", keyword.trim());

    // 별도 도감에 있을 때는 그 안에서 찾는다.
    // (여기서 홈으로 튕기면 "왜 작품이 나오지?" 하게 된다)
    for (const c of CATEGORIES) {
      // searchable 이 명시적으로 false 인 화면은 건너뛴다 → 아래 홈 검색으로 떨어진다
      // 상세(/character/1)에서 검색하면 목록(/characters)으로 — c.path 로 보내는 이유
      if (c.ready && c.searchable !== false && matchesPath(c, pathname)) {
        navigate(`${c.path}?${params.toString()}`);
        return;
      }
    }

    if (currentType) params.set("type", currentType); // 카테고리는 유지
    navigate(`/?${params.toString()}`);
  };

  /** 카테고리 전환 — 검색어는 유지하고 type만 바꾼다 */
  const selectCategory = (index: number) => {
    const category = CATEGORIES[index];
    if (!category.ready) return;

    const params = new URLSearchParams();
    if (currentKeyword) params.set("q", currentKeyword); // 검색어는 탭을 옮겨도 유지

    // 별도 경로를 가진 탭(캐릭터 등)
    if (category.path !== "/") {
      navigate(`${category.path}?${params.toString()}`);
      return;
    }

    if (category.type) params.set("type", category.type);
    navigate(`/?${params.toString()}`);
  };

  // 전역 인증 상태 — 로그인하면 헤더가 자동으로 다시 그려진다
  const { user, isLoggedIn, logout } = useAuth();

  // 아바타 드롭다운 메뉴의 기준 요소(anchor). null이면 닫힌 상태.
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const closeMenu = () => setMenuAnchor(null);

  const handleLogout = () => {
    closeMenu();
    logout(); // 토큰·회원정보 제거 → isLoggedIn=false → 헤더가 '로그인'으로 복귀
    navigate("/");
  };

  return (
    <Box sx={{ minHeight: "100vh", display: "flex", flexDirection: "column", bgcolor: "background.default" }}>
      <AppBar position="sticky" elevation={0} sx={{ bgcolor: "#1b2a4a" }}>
        <Toolbar sx={{ gap: 2, minHeight: { xs: 64, sm: 72 } }}>
          {/* 로고 — 클릭 시 홈으로 */}
          <Typography
            component="div"
            onClick={() => navigate("/")}
            sx={{ fontWeight: 800, fontSize: { xs: 22, sm: 27 }, letterSpacing: "-0.5px", cursor: "pointer", whiteSpace: "nowrap" }}
          >
            Haru<span style={{ color: "#38bdf8" }}>Verse</span>
          </Typography>

          {/* 검색바 */}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              bgcolor: "rgba(255,255,255,0.15)",
              "&:hover": { bgcolor: "rgba(255,255,255,0.25)" },
              borderRadius: 2,
              px: 2,
              py: 0.75,
              ml: { xs: 1, sm: 3 },
              flex: 1,
              maxWidth: 520,
              position: "relative", // 후보 목록의 기준점
            }}
            ref={setSearchBoxEl}
          >
            <SearchIcon
              onClick={submitSearch}
              sx={{ color: "rgba(255,255,255,0.7)", fontSize: 26, cursor: "pointer" }}
            />
            <InputBase
              placeholder="작품 검색…"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setSuggestOpen(true);
                setHighlighted(-1); // 글자가 바뀌면 선택을 푼다
              }}
              onFocus={() => setSuggestOpen(true)}
              // Enter로 검색 (IME 조합 중 Enter는 무시 — 한글 입력 확정용 Enter가
              // 검색으로 새지 않게 한다)
              onKeyDown={(e) => {
                // ★IME 조합 중 Enter는 무시★ 한글 확정용 Enter가 검색으로 새지 않게 한다.
                //   (조합 중에는 방향키도 후보 이동이 아니라 글자 선택에 쓰인다)
                if (e.nativeEvent.isComposing) return;

                if (e.key === "ArrowDown") {
                  e.preventDefault();
                  setSuggestOpen(true);
                  setHighlighted((i) => (i + 1) % Math.max(suggestions.length, 1));
                } else if (e.key === "ArrowUp") {
                  e.preventDefault();
                  setHighlighted((i) => (i <= 0 ? suggestions.length - 1 : i - 1));
                } else if (e.key === "Escape") {
                  setSuggestOpen(false);
                  setHighlighted(-1);
                } else if (e.key === "Enter") {
                  e.preventDefault();
                  // 후보를 골라둔 상태면 그 작품으로, 아니면 평소대로 검색
                  if (highlighted >= 0 && suggestions[highlighted]) {
                    chooseSuggestion(suggestions[highlighted].id);
                  } else {
                    setSuggestOpen(false);
                    clearSuggestions();
                    submitSearch();
                  }
                }
              }}
              inputProps={{ "aria-label": "작품 검색" }}
              sx={{
                color: "#fff",
                ml: 1.5,
                flex: 1,
                fontSize: 16,
                "& input::placeholder": { color: "rgba(255,255,255,0.7)", opacity: 1 },
              }}
            />
            {/* 검색어가 있을 때만 지우기 버튼 */}
            {keyword && (
              <IconButton
                aria-label="검색어 지우기"
                onClick={() => {
                  setKeyword("");
                  const params = new URLSearchParams();
                  if (currentType) params.set("type", currentType);
                  navigate(`/?${params.toString()}`);
                }}
                sx={{ color: "rgba(255,255,255,0.7)", p: 0.5 }}
              >
                <CloseIcon sx={{ fontSize: 20 }} />
              </IconButton>
            )}

            {/* 자동완성 후보 — 검색창 바로 아래 */}
            <Popper
              open={suggestOpen && suggestions.length > 0}
              anchorEl={searchBoxEl}
              placement="bottom-start"
              // ★AppBar보다 위로★ 헤더가 z-index를 갖고 있어 그냥 두면 목록이 가려진다
              sx={{ zIndex: (theme) => theme.zIndex.appBar + 1, width: searchBoxEl?.clientWidth }}
            >
              <ClickAwayListener onClickAway={() => setSuggestOpen(false)}>
                <Paper elevation={8} sx={{ mt: 0.5, borderRadius: 2, overflow: "hidden" }}>
                  <MenuList dense disablePadding>
                    {suggestions.map((s, i) => (
                      <MenuItem
                        key={s.id}
                        selected={i === highlighted}
                        onMouseEnter={() => setHighlighted(i)}
                        onClick={() => chooseSuggestion(s.id)}
                        sx={{ py: 1, gap: 1.5 }}
                      >
                        {/* 썸네일 — 제목만 있으면 시리즈물을 구분하기 어렵다 */}
                        <Box
                          sx={{
                            width: 30, height: 42, flexShrink: 0, borderRadius: 0.5,
                            bgcolor: "grey.200",
                            backgroundImage: s.imageUrl ? `url(${s.imageUrl})` : undefined,
                            backgroundSize: "cover",
                            backgroundPosition: "center",
                          }}
                        />
                        <Box sx={{ minWidth: 0 }}>
                          <Typography
                            variant="body2"
                            sx={{ fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
                          >
                            {s.title}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {s.type === "GAME" ? "게임" : "애니메이션"}
                          </Typography>
                        </Box>
                      </MenuItem>
                    ))}
                  </MenuList>
                </Paper>
              </ClickAwayListener>
            </Popper>
          </Box>

          {/* 오른쪽으로 밀어내는 스페이서 */}
          <Box sx={{ flexGrow: 1 }} />

          {/* 인증 영역 — 로그인 여부에 따라 갈라진다 */}
          {isLoggedIn ? (
            /* 로그인 상태: 닉네임 + 아바타 → 클릭 시 마이페이지/로그아웃 메뉴 */
            <>
              <Button
                onClick={(e) => setMenuAnchor(e.currentTarget)}
                aria-label="계정 메뉴"
                sx={{
                  color: "#fff",
                  fontWeight: 700,
                  borderRadius: 2,
                  px: 1.2,
                  py: 0.6,
                  gap: 1,
                  textTransform: "none",
                  whiteSpace: "nowrap",
                  "&:hover": { bgcolor: "rgba(56,189,248,0.12)" },
                }}
              >
                <Avatar sx={{ width: 32, height: 32, bgcolor: "#38bdf8", color: "#0f1a2e", fontWeight: 800, fontSize: 15 }}>
                  {/* 닉네임 첫 글자를 아바타로 */}
                  {user?.nickname?.charAt(0) ?? "?"}
                </Avatar>
                {/* 닉네임은 좁은 화면에선 숨김 (아바타만 노출) */}
                <Box component="span" sx={{ display: { xs: "none", sm: "inline" }, fontSize: 15 }}>
                  {user?.nickname}
                </Box>
              </Button>

              <Menu
                anchorEl={menuAnchor}
                open={Boolean(menuAnchor)}
                onClose={closeMenu}
                anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
                transformOrigin={{ vertical: "top", horizontal: "right" }}
                slotProps={{ paper: { sx: { mt: 1, minWidth: 190, borderRadius: 2 } } }}
              >
                {/* 누구로 로그인했는지 확인용 (클릭 불가) */}
                <Box sx={{ px: 2, py: 1.2 }}>
                  <Typography sx={{ fontWeight: 700, fontSize: 14, color: "#1b2a4a" }}>{user?.nickname}</Typography>
                  <Typography sx={{ fontSize: 12, color: "text.secondary" }}>{user?.email}</Typography>
                </Box>
                <Divider />

                <MenuItem
                  onClick={() => {
                    closeMenu();
                    navigate("/mypage");
                  }}
                >
                  <ListItemIcon>
                    <PersonOutlineIcon fontSize="small" />
                  </ListItemIcon>
                  마이페이지
                </MenuItem>

                <MenuItem onClick={handleLogout} sx={{ color: "#d32f2f" }}>
                  <ListItemIcon>
                    <LogoutIcon fontSize="small" sx={{ color: "#d32f2f" }} />
                  </ListItemIcon>
                  로그아웃
                </MenuItem>
              </Menu>
            </>
          ) : (
            /* 비로그인 상태: 로그인 화면으로 보내는 버튼 */
            <Button
              variant="outlined"
              onClick={() => navigate("/login")}
              sx={{
                color: "#fff",
                borderColor: "rgba(255,255,255,0.5)",
                fontWeight: 700,
                borderRadius: 2,
                px: 2.4,
                py: 0.7,
                whiteSpace: "nowrap",
                "&:hover": { borderColor: "#38bdf8", color: "#38bdf8", bgcolor: "rgba(56,189,248,0.12)" },
              }}
            >
              로그인
            </Button>
          )}
        </Toolbar>

        {/* 카테고리 탭 바 — 시안 인디케이터 */}
        <Box sx={{ bgcolor: "#16233d", px: { xs: 0.5, sm: 2 } }}>
          <Tabs
            value={tab}
            onChange={(_, v) => selectCategory(v)}
            variant="scrollable"
            scrollButtons="auto"
            allowScrollButtonsMobile
            sx={{
              minHeight: 44,
              "& .MuiTab-root": {
                color: "rgba(255,255,255,0.72)",
                fontWeight: 700,
                fontSize: 14,
                minHeight: 44,
                py: 0,
              },
              "& .Mui-selected": { color: "#38bdf8 !important" },
              "& .MuiTabs-indicator": { backgroundColor: "#38bdf8", height: 3 },
              "& .MuiTabs-scrollButtons": { color: "rgba(255,255,255,0.6)" },
            }}
          >
            {CATEGORIES.map((c) => (
              <Tab
                key={c.label}
                label={c.label}
                disabled={!c.ready}
                sx={!c.ready ? { opacity: 0.35 } : undefined}
              />
            ))}
          </Tabs>
        </Box>
      </AppBar>

      {/* 본문 — 페이지 내용이 여기 (풀 와이드: 화면 폭에 꽉 차게) */}
      <Container maxWidth={false} sx={{ py: 4, px: { xs: 2, sm: 3, md: 4 }, flexGrow: 1 }}>
        {children}
      </Container>

      {/* 푸터 — 화면 하단에 붙음 (flexGrow로 본문이 밀어냄) */}
      <Box
        component="footer"
        sx={{
          mt: 6,
          color: "rgba(255,255,255,0.72)",
          background: "linear-gradient(180deg, #16233d 0%, #0f1a2e 100%)",
        }}
      >
        {/* 상단 시안 그라데이션 액센트 라인 */}
        <Box sx={{ height: 3, background: "linear-gradient(90deg, #38bdf8, #2563eb)" }} />

        <Box sx={{ px: { xs: 3, md: 6 }, py: 6 }}>
          {/* 상단: 브랜드 + 링크 컬럼들 */}
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr", md: "2.2fr 1fr 1fr 1.3fr" },
              gap: { xs: 4, md: 5 },
            }}
          >
            {/* 브랜드 소개 + 기술 스택 */}
            <Box>
              <Typography variant="h5" sx={{ fontWeight: 800, color: "#fff", letterSpacing: "-0.5px" }}>
                Haru<span style={{ color: "#38bdf8" }}>Verse</span>
              </Typography>
              <Typography variant="body2" sx={{ mt: 1.5, color: "rgba(255,255,255,0.6)", maxWidth: 340, lineHeight: 1.75 }}>
                애니메이션과 게임을 한곳에서 검색하는 통합 도감.
                좋아하는 작품을 찾고, 기록하고, 공유하세요.
              </Typography>
            </Box>

            {/* 탐색 링크 */}
            <FooterLinkColumn
              title="탐색"
              items={[
                { label: "홈", to: "/" },
                { label: "애니메이션", to: "/?type=ANIME" },
                { label: "게임", to: "/?type=GAME" },
                { label: "캐릭터", to: "/characters" },
                { label: "성우", to: "/voice-actors" },
                { label: "커뮤니티", to: "/community" },
                { label: "제작사", to: "/studios" },
              ]}
            />

            {/* 정보 링크 */}
            <FooterLinkColumn
              title="정보"
              items={[
                { label: "소개", to: "/about" },
                { label: "이용약관", to: "/terms" },
                { label: "개인정보처리방침", to: "/privacy" },
                { label: "문의", to: "/contact" },
              ]}
            />

            {/* 데이터 출처 + 소셜 — 전부 외부 링크라 새 탭으로 연다 */}
            <FooterLinkColumn
              title="데이터 · 소셜"
              items={[
                { label: "Jikan API (애니)", href: SITE.jikan },
                { label: "RAWG (게임)", href: SITE.rawg },
              ]}
              extra={
                <IconButton
                  href={SITE.github}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="GitHub"
                  sx={{
                    mt: 2,
                    color: "rgba(255,255,255,0.8)",
                    border: "1px solid rgba(255,255,255,0.2)",
                    borderRadius: 2,
                    "&:hover": { color: "#38bdf8", borderColor: "#38bdf8", bgcolor: "rgba(56,189,248,0.08)" },
                  }}
                >
                  <GitHubIcon />
                </IconButton>
              }
            />
          </Box>

          {/* 구분선 */}
          <Divider sx={{ my: 4, borderColor: "rgba(255,255,255,0.10)" }} />

          {/* 하단 바 — 저작권 + 제작 정보 */}
          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1, justifyContent: "space-between", alignItems: "center" }}>
            <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.5)" }}>
              © 2026 HaruVerse — Personal project by Haru.
            </Typography>
            <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.45)" }}>
              Made with React · Spring Boot · 맥미니 셀프호스팅
            </Typography>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}

export default Layout;
