import { useEffect, useState } from "react";
import {
  Box, Typography, Pagination, Alert, Button, Stack, Skeleton,
  Table, TableBody, TableCell, TableHead, TableRow, Chip,
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import EditIcon from "@mui/icons-material/Edit";
import ChatBubbleOutlineIcon from "@mui/icons-material/ChatBubbleOutlineOutlined";
import ThumbUpOutlinedIcon from "@mui/icons-material/ThumbUpOutlined";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import SubscribeButton from "../../components/SubscribeButton";
import { useAuth } from "../../contexts/AuthContext";
import { fetchWork } from "../work/api";
import { fetchPosts, formatPostDate } from "./api";
import type { PostSummary } from "./api";

const PAGE_SIZE = 20;

/**
 * 작품 게시판 — /work/:workId/posts
 *
 * <p><b>게시판이 곧 작품이다</b>
 * 별도의 채널 엔티티 없이 Work 를 게시판으로 쓴다. 그래서 이 화면은
 * 작품 제목을 머리에 두고, 목록은 흔한 게시판 표 형태로 보여준다.
 */
function PostListPage() {
  const { workId: workIdParam } = useParams<{ workId: string }>();
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // URL 파라미터 검증은 렌더 중에 (effect 안에서 setState 하면 렌더가 한 번 더 돈다)
  const workId = Number(workIdParam);
  const invalidId = !workIdParam || !Number.isFinite(workId);
  const page = Number(searchParams.get("page") ?? "1"); // MUI Pagination은 1부터

  const [workTitle, setWorkTitle] = useState("");
  const [posts, setPosts] = useState<PostSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (invalidId) return;
    let alive = true;

    (async () => {
      setLoading(true);
      setError("");
      try {
        // 작품 제목과 글 목록을 함께 — 순차로 부르면 화면이 두 번 늦게 뜬다
        const [work, res] = await Promise.all([
          fetchWork(workId),
          fetchPosts(workId, page - 1, PAGE_SIZE),
        ]);
        if (!alive) return;
        setWorkTitle(work.title);
        setPosts(res.content);
        setTotal(res.totalElements);
        setTotalPages(res.totalPages);
      } catch (e) {
        if (alive) setError(e instanceof Error ? e.message : "게시판을 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [workId, invalidId, page]);

  if (invalidId) {
    return (
      <Layout>
        <Alert severity="error" sx={{ mt: 4 }}>잘못된 게시판 주소입니다.</Alert>
      </Layout>
    );
  }

  const goToPage = (next: number) => {
    const params = new URLSearchParams(searchParams);
    if (next <= 1) params.delete("page");
    else params.set("page", String(next));
    setSearchParams(params);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  /** 글쓰기 — 비로그인이면 로그인 화면으로 (버튼을 숨기지 않는 이유는 찜과 같다) */
  const goWrite = () => {
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }
    navigate(`/work/${workId}/posts/new`);
  };

  return (
    <Layout>
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate(`/work/${workId}`)}
        sx={{ mt: 2, color: "text.secondary" }}
      >
        작품으로
      </Button>

      <Stack
        direction="row"
        sx={{ mt: 1, mb: 2, alignItems: "flex-end", justifyContent: "space-between", flexWrap: "wrap", gap: 2 }}
      >
        <Box>
          <Typography
            variant="h5"
            sx={{
              fontWeight: 800, color: "#1b2a4a", position: "relative", pl: 1.5,
              "&::before": {
                content: '""', position: "absolute", left: 0, top: 5, bottom: 5,
                width: 4, borderRadius: 2, bgcolor: "#38bdf8",
              },
            }}
          >
            {loading && !workTitle ? <Skeleton width={280} /> : `${workTitle} 게시판`}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, pl: 1.5 }}>
            전체 <Box component="b" sx={{ color: "#0891b2" }}>{total}</Box>개의 글
          </Typography>
        </Box>

        {/* 구독은 "이 게시판을 계속 보겠다", 글쓰기는 "지금 쓴다" — 나란히 둔다 */}
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <SubscribeButton workId={workId} size="medium" />
          <Button variant="contained" startIcon={<EditIcon />} onClick={goWrite} sx={{ fontWeight: 700 }}>
            글쓰기
          </Button>
        </Stack>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Box sx={{ border: "1px solid #e5eaf2", borderRadius: 2, overflow: "hidden", bgcolor: "#fff" }}>
        <Table size="small">
          <TableHead>
            <TableRow sx={{ bgcolor: "#f6f8fc" }}>
              <TableCell sx={{ fontWeight: 800, color: "#1b2a4a" }}>제목</TableCell>
              <TableCell sx={{ fontWeight: 800, color: "#1b2a4a", width: 120 }}>작성자</TableCell>
              <TableCell sx={{ fontWeight: 800, color: "#1b2a4a", width: 110, whiteSpace: "nowrap" }}>작성일</TableCell>
              <TableCell align="right" sx={{ fontWeight: 800, color: "#1b2a4a", width: 70 }}>조회</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading
              ? Array.from({ length: 5 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell colSpan={4}><Skeleton height={28} /></TableCell>
                  </TableRow>
                ))
              : posts.map((p) => (
                  <TableRow
                    key={p.id}
                    hover
                    onClick={() => navigate(`/post/${p.id}`)}
                    sx={{ cursor: "pointer" }}
                  >
                    <TableCell>
                      <Stack direction="row" spacing={1} sx={{ alignItems: "center", flexWrap: "wrap" }}>
                        <Typography sx={{ fontWeight: 600, color: "#1b2a4a" }}>{p.title}</Typography>
                        {/* 댓글·추천은 0이면 표시하지 않는다 — 빈 숫자가 목록을 어지럽힌다 */}
                        {p.commentCount > 0 && (
                          <Chip
                            size="small"
                            icon={<ChatBubbleOutlineIcon sx={{ fontSize: 12 }} />}
                            label={p.commentCount}
                            sx={{ height: 20, fontSize: 11, bgcolor: "rgba(56,189,248,0.12)", color: "#0891b2" }}
                          />
                        )}
                        {p.likeCount > 0 && (
                          <Chip
                            size="small"
                            icon={<ThumbUpOutlinedIcon sx={{ fontSize: 12 }} />}
                            label={p.likeCount}
                            sx={{ height: 20, fontSize: 11, bgcolor: "rgba(244,63,94,0.10)", color: "#f43f5e" }}
                          />
                        )}
                      </Stack>
                    </TableCell>
                    <TableCell sx={{ color: "text.secondary" }}>{p.authorNickname}</TableCell>
                    <TableCell sx={{ color: "text.secondary", whiteSpace: "nowrap" }}>
                      {formatPostDate(p.createdAt)}
                    </TableCell>
                    <TableCell align="right" sx={{ color: "text.secondary" }}>{p.viewCount}</TableCell>
                  </TableRow>
                ))}
          </TableBody>
        </Table>

        {!loading && !error && posts.length === 0 && (
          <Box sx={{ py: 8, textAlign: "center" }}>
            <Typography color="text.secondary">아직 글이 없어요.</Typography>
            <Typography variant="caption" color="text.secondary">첫 글을 남겨보세요.</Typography>
          </Box>
        )}
      </Box>

      {totalPages > 1 && (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 4, mb: 2 }}>
          <Pagination
            count={totalPages}
            page={page}
            onChange={(_, v) => goToPage(v)}
            shape="rounded"
            variant="outlined"
          />
        </Box>
      )}
    </Layout>
  );
}

export default PostListPage;
