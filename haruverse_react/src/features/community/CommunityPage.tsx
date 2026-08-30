import { useEffect, useState } from "react";
import { Box, Typography, Pagination, Alert, Skeleton, TextField, InputAdornment, IconButton, Card, Stack, Divider } from "@mui/material";
import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import SearchIcon from "@mui/icons-material/Search";
import CloseIcon from "@mui/icons-material/Close";
import NotificationsActiveIcon from "@mui/icons-material/NotificationsActive";
import { useNavigate, useSearchParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import { apiFetch } from "../../lib/api";
import { useAuth } from "../../contexts/AuthContext";
import { useSubscription } from "../../contexts/SubscriptionContext";
import ChannelCard from "./ChannelCard";
import { fetchMySubscriptions } from "./subscriptionApi";
import { formatPostDate, searchPosts } from "./api";
import type { Channel, RecentPost } from "./api";
import type { PageResponse } from "../work/types";

const PAGE_SIZE = 24;

function fetchChannels(page = 0, size = PAGE_SIZE): Promise<PageResponse<Channel>> {
  return apiFetch<PageResponse<Channel>>(`/api/community/channels?page=${page}&size=${size}`);
}

/** 채널 카드 격자 — 구독 섹션과 전체 목록이 같은 배치를 쓴다 */
function ChannelGrid({ children }: { children: React.ReactNode }) {
  return (
    <Box
      sx={{
        display: "grid",
        gridTemplateColumns: {
          xs: "repeat(1, 1fr)",
          sm: "repeat(2, 1fr)",
          lg: "repeat(3, 1fr)",
        },
        gap: 2,
      }}
    >
      {children}
    </Box>
  );
}

/** 좌측에 색 막대가 있는 섹션 제목 */
function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <Typography
      variant="h5"
      sx={{
        fontWeight: 800, color: "#1b2a4a", position: "relative", pl: 1.5, mt: 4, mb: 1,
        display: "flex", alignItems: "center", gap: 1,
        "&::before": {
          content: '""', position: "absolute", left: 0, top: 5, bottom: 5,
          width: 4, borderRadius: 2, bgcolor: "#38bdf8",
        },
      }}
    >
      {children}
    </Typography>
  );
}

/**
 * 커뮤니티 — /community
 *
 * <p><b>채널 = 작품</b>. 작품이 187개지만 카드가 되는 건
 * <b>글이 하나라도 있는 채널</b>뿐이다 (빈 채널을 다 깔면 화면이 죽는다).
 *
 * <p>로그인했고 구독한 채널이 있으면 <b>맨 위에 "내 구독 채널"</b>이 따로 붙는다.
 * 구독의 보상이 눈에 보이지 않으면 아무도 구독 버튼을 누르지 않는다.
 */
function CommunityPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get("page") ?? "1"); // MUI Pagination은 1부터

  const { isLoggedIn } = useAuth();
  // 구독 id 집합이 바뀌면(다른 화면에서 구독/해제) 이 목록도 다시 받아야 한다
  const { subscribedIds } = useSubscription();

  const [channels, setChannels] = useState<Channel[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // ── 게시글 검색 ──
  //   검색어가 있으면 채널 격자 대신 글 목록을 보여준다.
  //   주소(?q=)에 담아 두어 새로고침·뒤로가기에도 검색 상태가 남는다.
  const navigate = useNavigate();
  // ★파라미터 이름이 q 가 아니라 post 다★
  //   헤더의 작품 검색창이 ?q= 를 자기 검색어로 읽는다(Layout.tsx).
  //   커뮤니티에서 q 를 쓰면 헤더 입력창에 게시글 검색어가 나타나고,
  //   거기서 지우면 작품 목록(/)으로 튕긴다 — 실제로 그렇게 동작했다.
  //   의미가 다른 검색이므로 파라미터도 분리한다.
  const query = searchParams.get("post") ?? "";
  const [keyword, setKeyword] = useState(query);
  const [results, setResults] = useState<RecentPost[]>([]);
  const [resultTotal, setResultTotal] = useState(0);
  const [searching, setSearching] = useState(false);

  const [mine, setMine] = useState<Channel[]>([]);
  const [mineLoading, setMineLoading] = useState(isLoggedIn);

  useEffect(() => {
    let alive = true;

    (async () => {
      setLoading(true);
      setError("");
      try {
        const res = await fetchChannels(page - 1, PAGE_SIZE);
        if (!alive) return;
        setChannels(res.content);
        setTotal(res.totalElements);
        setTotalPages(res.totalPages);
      } catch (e) {
        if (alive) setError(e instanceof Error ? e.message : "채널을 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [page]);

  // ★로그아웃 순간의 처리는 useEffect가 아니라 렌더 중에 조정한다★
  //   effect로 비우면 한 프레임 동안 남의 구독 채널이 보이고,
  //   react-hooks/set-state-in-effect 린트에도 걸린다.
  //   (FavoriteProvider와 같은 패턴)
  const [syncedLoggedIn, setSyncedLoggedIn] = useState(isLoggedIn);
  if (isLoggedIn !== syncedLoggedIn) {
    setSyncedLoggedIn(isLoggedIn);
    if (!isLoggedIn) setMine([]);
    setMineLoading(isLoggedIn);
  }

  // 내 구독 채널.
  // subscribedIds를 의존성에 넣는 이유 — 이 화면의 카드에서 구독을 풀면
  // 위 섹션에서도 사라져야 한다. 넣지 않으면 새로고침해야 반영된다.
  useEffect(() => {
    if (!isLoggedIn) return;
    let alive = true;

    (async () => {
      setMineLoading(true);
      try {
        const res = await fetchMySubscriptions();
        if (alive) setMine(res);
      } catch {
        // 구독 목록을 못 받아도 아래 전체 채널은 보여야 한다
      } finally {
        if (alive) setMineLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [isLoggedIn, subscribedIds]);

  // ★입력창 동기화는 effect 가 아니라 렌더 중에 한다★
  //   뒤로가기로 주소의 q 가 바뀌면 입력창도 따라가야 하는데,
  //   effect 에서 setState 를 부르면 한 번 그린 뒤 다시 그리게 된다(React 19 가 막는다).
  //   이 파일의 syncedLoggedIn 과 같은 방식이다.
  const [syncedQuery, setSyncedQuery] = useState(query);
  if (syncedQuery !== query) {
    setSyncedQuery(query);
    setKeyword(query);
  }

  // 주소의 q 가 바뀌면 검색한다 (입력할 때마다가 아니라 '검색'을 눌렀을 때만 바뀐다)
  useEffect(() => {
    // 검색 중이 아니면 결과 섹션 자체를 그리지 않으므로 비울 필요가 없다
    if (!query) return;

    let alive = true;

    // 이 파일의 채널 로딩과 같은 모양 — effect 본문에서 바로 setState 하지 않고
    // 즉시 실행 async 안에서 부른다(React 19 의 set-state-in-effect 규칙)
    (async () => {
      setSearching(true);
      setError("");
      try {
        const res = await searchPosts(query, 0, 30);
        if (!alive) return;
        setResults(res.content);
        setResultTotal(res.totalElements);
      } catch (e) {
        if (alive) setError(e instanceof Error ? e.message : "검색에 실패했습니다.");
      } finally {
        if (alive) setSearching(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [query]);

  const submitSearch = () => {
    const params = new URLSearchParams(searchParams);
    params.delete("page"); // 검색하면 1페이지부터
    if (keyword.trim()) params.set("post", keyword.trim());
    else params.delete("post");
    setSearchParams(params);
  };

  const clearSearch = () => {
    setKeyword("");
    const params = new URLSearchParams(searchParams);
    params.delete("post");
    setSearchParams(params);
  };

  const goToPage = (next: number) => {
    const params = new URLSearchParams(searchParams);
    if (next <= 1) params.delete("page");
    else params.set("page", String(next));
    setSearchParams(params);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <Layout>
      {/* ── 게시글 검색 ── */}
      <TextField
        fullWidth
        size="small"
        placeholder="게시글 검색 — 제목 · 본문 · 작성자 · 작품명"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && submitSearch()}
        sx={{ mb: 3, bgcolor: "#fff" }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon sx={{ color: "#94a3b8" }} />
              </InputAdornment>
            ),
            endAdornment: keyword ? (
              <InputAdornment position="end">
                <IconButton size="small" onClick={clearSearch} aria-label="검색어 지우기">
                  <CloseIcon sx={{ fontSize: 18 }} />
                </IconButton>
              </InputAdornment>
            ) : undefined,
          },
        }}
      />

      {/* ── 검색 결과 — 검색 중에는 채널 격자를 감춘다 ── */}
      {query && (
        <Box sx={{ mb: 4 }}>
          <SectionTitle>검색 결과</SectionTitle>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2, pl: 1.5 }}>
            "{query}" · <Box component="b" sx={{ color: "#0891b2" }}>{resultTotal}</Box>건
          </Typography>

          {searching ? (
            <Skeleton variant="rounded" height={180} />
          ) : results.length === 0 ? (
            <Box sx={{ py: 8, textAlign: "center" }}>
              <ForumOutlinedIcon sx={{ fontSize: 40, color: "#e5eaf2" }} />
              <Typography color="text.secondary" sx={{ mt: 1 }}>
                검색 결과가 없어요.
              </Typography>
            </Box>
          ) : (
            <Card elevation={0} sx={{ borderRadius: 3, border: "1px solid #e5eaf2", bgcolor: "#fff" }}>
              {results.map((r, i) => (
                <Box key={r.id}>
                  {i > 0 && <Divider />}
                  <Box
                    onClick={() => navigate(`/post/${r.id}`)}
                    sx={{
                      p: 2,
                      cursor: "pointer",
                      "&:hover": { bgcolor: "#f8fafc" },
                    }}
                  >
                    <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 0.5 }}>
                      <Typography
                        variant="caption"
                        sx={{
                          color: "#0891b2", bgcolor: "rgba(56,189,248,0.12)",
                          px: 0.8, py: 0.2, borderRadius: 1, fontWeight: 700,
                        }}
                      >
                        {r.workTitle}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {r.authorNickname} · {formatPostDate(r.createdAt)}
                      </Typography>
                    </Stack>
                    <Typography sx={{ fontWeight: 700, color: "#1b2a4a" }}>
                      {r.title}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      조회 {r.viewCount} · 댓글 {r.commentCount} · 추천 {r.likeCount}
                    </Typography>
                  </Box>
                </Box>
              ))}
            </Card>
          )}
        </Box>
      )}

      {/* ── 내 구독 채널 — 로그인했고 구독이 하나라도 있을 때만 ── */}
      {!query && isLoggedIn && (mineLoading || mine.length > 0) && (
        <Box sx={{ mb: 4 }}>
          <SectionTitle>
            <NotificationsActiveIcon sx={{ color: "#0891b2" }} />
            내 구독 채널
          </SectionTitle>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2, pl: 1.5 }}>
            새 글이 올라온 채널부터 보여드려요
          </Typography>

          <ChannelGrid>
            {mineLoading
              ? Array.from({ length: 3 }).map((_, i) => (
                  <Skeleton key={i} variant="rounded" height={148} />
                ))
              : mine.map((c) => <ChannelCard key={c.workId} channel={c} />)}
          </ChannelGrid>
        </Box>
      )}

      {/* ── 전체 채널 — 검색 중에는 감춘다 ── */}
      {!query && (
      <>
      <SectionTitle>{isLoggedIn && mine.length > 0 ? "전체 채널" : "커뮤니티"}</SectionTitle>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, pl: 1.5 }}>
        글이 있는 채널 <Box component="b" sx={{ color: "#0891b2" }}>{total}</Box>개 · 최근 글 순
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <ChannelGrid>
        {loading
          ? Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} variant="rounded" height={148} />
            ))
          : channels.map((c) => <ChannelCard key={c.workId} channel={c} />)}
      </ChannelGrid>

      {!loading && !error && channels.length === 0 && (
        <Box sx={{ py: 8, textAlign: "center" }}>
          <ForumOutlinedIcon sx={{ fontSize: 40, color: "#e5eaf2" }} />
          <Typography color="text.secondary" sx={{ mt: 1 }}>아직 글이 있는 채널이 없어요.</Typography>
          <Typography variant="caption" color="text.secondary">
            작품 상세에서 게시판에 들어가 첫 글을 남겨보세요.
          </Typography>
        </Box>
      )}

      {totalPages > 1 && (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 4, mb: 2 }}>
          <Pagination
            count={totalPages}
            page={page}
            onChange={(_, v) => goToPage(v)}
            shape="rounded"
            size="large"
            variant="outlined"
            sx={{
              "& .MuiPaginationItem-root": { border: "1px solid #cfd8e3", fontSize: 16, minWidth: 42, height: 42 },
              "& .MuiPaginationItem-root.Mui-selected": {
                bgcolor: "#38bdf8", borderColor: "#38bdf8", color: "#fff",
                "&:hover": { bgcolor: "#0ea5e9" },
              },
            }}
          />
        </Box>
      )}
      </>
      )}
    </Layout>
  );
}

export default CommunityPage;
