import { useEffect, useState } from "react";
import { Box, Typography, Pagination, Alert, Skeleton } from "@mui/material";
import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import NotificationsActiveIcon from "@mui/icons-material/NotificationsActive";
import { useSearchParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import { apiFetch } from "../../lib/api";
import { useAuth } from "../../contexts/AuthContext";
import { useSubscription } from "../../contexts/SubscriptionContext";
import ChannelCard from "./ChannelCard";
import { fetchMySubscriptions } from "./subscriptionApi";
import type { Channel } from "./api";
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

  const goToPage = (next: number) => {
    const params = new URLSearchParams(searchParams);
    if (next <= 1) params.delete("page");
    else params.set("page", String(next));
    setSearchParams(params);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <Layout>
      {/* ── 내 구독 채널 — 로그인했고 구독이 하나라도 있을 때만 ── */}
      {isLoggedIn && (mineLoading || mine.length > 0) && (
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

      {/* ── 전체 채널 ── */}
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
    </Layout>
  );
}

export default CommunityPage;
