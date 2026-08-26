import { useEffect, useState } from "react";
import { Box, Typography, Card, CardActionArea, Chip, Stack, Divider, Button, Skeleton } from "@mui/material";
import StarIcon from "@mui/icons-material/Star";
import ChatBubbleOutlineIcon from "@mui/icons-material/ChatBubbleOutlineOutlined";
import ThumbUpOutlinedIcon from "@mui/icons-material/ThumbUpOutlined";
import { useNavigate } from "react-router-dom";
import FavoriteButton from "../../components/FavoriteButton";
import SubscribeButton from "../../components/SubscribeButton";
import { fetchWork } from "../work/api";
import type { WorkDetail } from "../work/types";
import { fetchPosts, formatPostDate } from "./api";
import type { PostSummary } from "./api";

/** 사이드바에 보여줄 다른 글 수 (현재 글을 뺀 뒤 기준) */
const OTHER_POSTS = 5;

/**
 * 글 상세의 사이드바 — 작품 정보 + 같은 채널의 다른 글.
 *
 * <p><b>왜 이 둘인가</b>
 * ① 작품 정보는 "어느 작품 이야기인지" 맥락을 준다. 도감과 커뮤니티를 잇는 자리이기도 하다.
 * ② 같은 채널의 다른 글은 "다음 글로" 이어지게 한다 — 글 하나 읽고 나가버리는 걸 막는다.
 *
 * <p>둘 다 이미 있는 API로 만든다 (새 쿼리 없음).
 */
function PostSidebar({ workId, currentPostId }: { workId: number; currentPostId: number }) {
  const navigate = useNavigate();

  const [work, setWork] = useState<WorkDetail | null>(null);
  const [posts, setPosts] = useState<PostSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;

    // 두 요청을 함께 — 순차로 부르면 사이드바가 두 번 늦게 뜬다
    Promise.all([fetchWork(workId), fetchPosts(workId, 0, OTHER_POSTS + 1)])
      .then(([w, res]) => {
        if (!alive) return;
        setWork(w);
        // 지금 보고 있는 글은 빼고, 그래서 한 개 더 받아왔다
        setPosts(res.content.filter((p) => p.id !== currentPostId).slice(0, OTHER_POSTS));
      })
      .catch(() => {
        // 사이드바를 못 불러와도 본문은 읽을 수 있어야 한다
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [workId, currentPostId]);

  if (loading) {
    return (
      <Stack spacing={2}>
        <Skeleton variant="rounded" height={220} />
        <Skeleton variant="rounded" height={200} />
      </Stack>
    );
  }

  const isGame = work?.type === "GAME";

  return (
    <Stack spacing={2}>
      {/* ── 작품 정보 ── */}
      {work && (
        <Card elevation={0} sx={{ borderRadius: 2, border: "1px solid #e5eaf2", bgcolor: "#fff", p: 2 }}>
          <Box
            onClick={() => navigate(`/work/${work.id}`)}
            sx={{
              width: "100%",
              // 게임은 RAWG가 가로 이미지를 준다 (작품 상세와 같은 규칙)
              aspectRatio: isGame ? "16 / 9" : "2 / 3",
              maxHeight: isGame ? 160 : 300,
              borderRadius: 1.5,
              bgcolor: "grey.200",
              backgroundImage: work.imageUrl ? `url(${work.imageUrl})` : undefined,
              backgroundSize: "cover",
              backgroundPosition: "center",
              cursor: "pointer",
              transition: "outline .15s ease",
              "&:hover": { outline: "2px solid #38bdf8" },
            }}
          />

          <Typography
            sx={{ mt: 1.5, fontWeight: 800, color: "#1b2a4a", lineHeight: 1.35, cursor: "pointer" }}
            onClick={() => navigate(`/work/${work.id}`)}
          >
            {work.title}
          </Typography>

          <Stack direction="row" spacing={0.8} sx={{ mt: 1, flexWrap: "wrap", gap: 0.8, alignItems: "center" }}>
            <Chip
              label={isGame ? "게임" : "애니메이션"}
              size="small"
              color="primary"
              sx={{ height: 20, fontSize: 11, fontWeight: 700 }}
            />
            {work.rating != null && (
              <Chip
                size="small"
                icon={<StarIcon sx={{ fontSize: 12, color: "#fbbf24 !important" }} />}
                label={work.rating.toFixed(1)}
                sx={{ height: 20, fontSize: 11, bgcolor: "#0f1a2e", color: "#fff", fontWeight: 700 }}
              />
            )}
          </Stack>

          {work.genres.length > 0 && (
            <Stack direction="row" spacing={0.5} sx={{ mt: 1, flexWrap: "wrap", gap: 0.5 }}>
              {work.genres.slice(0, 4).map((g) => (
                <Chip
                  key={g}
                  label={g}
                  size="small"
                  onClick={() => navigate(`/?genre=${encodeURIComponent(g)}`)}
                  sx={{
                    height: 20, fontSize: 11, fontWeight: 600, cursor: "pointer",
                    bgcolor: "rgba(56,189,248,0.12)", color: "#0891b2",
                  }}
                />
              ))}
            </Stack>
          )}

          {/* 찜 = "작품이 좋다"(도감) / 구독 = "게시판 글을 읽겠다"(커뮤니티).
              뜻이 달라 따로 저장되므로 버튼도 둘이다 */}
          <Stack direction="row" spacing={1} sx={{ mt: 1.5, flexWrap: "wrap", gap: 1 }}>
            <FavoriteButton workId={work.id} variant="labeled" />
            <SubscribeButton workId={work.id} size="medium" />
          </Stack>
        </Card>
      )}

      {/* ── 같은 채널의 다른 글 ── */}
      <Card elevation={0} sx={{ borderRadius: 2, border: "1px solid #e5eaf2", bgcolor: "#fff" }}>
        <Typography sx={{ px: 2, pt: 2, pb: 1, fontWeight: 800, color: "#1b2a4a", fontSize: 14 }}>
          이 게시판의 다른 글
        </Typography>

        {posts.length === 0 ? (
          <Box sx={{ px: 2, pb: 2 }}>
            <Typography variant="caption" color="text.secondary">
              아직 다른 글이 없어요.
            </Typography>
          </Box>
        ) : (
          posts.map((p, i) => (
            <Box key={p.id}>
              {i === 0 && <Divider />}
              <CardActionArea onClick={() => navigate(`/post/${p.id}`)} sx={{ px: 2, py: 1.2 }}>
                <Typography
                  variant="body2"
                  sx={{
                    fontWeight: 600, color: "#1b2a4a",
                    overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
                  }}
                >
                  {p.title}
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mt: 0.3, alignItems: "center", color: "text.secondary" }}>
                  <Typography variant="caption">{p.authorNickname}</Typography>
                  <Typography variant="caption">{formatPostDate(p.createdAt)}</Typography>
                  {/* 0이면 표시하지 않는다 — 좁은 사이드바에서 빈 숫자는 소음이다 */}
                  {p.commentCount > 0 && (
                    <Stack direction="row" spacing={0.3} sx={{ alignItems: "center" }}>
                      <ChatBubbleOutlineIcon sx={{ fontSize: 11 }} />
                      <Typography variant="caption">{p.commentCount}</Typography>
                    </Stack>
                  )}
                  {p.likeCount > 0 && (
                    <Stack direction="row" spacing={0.3} sx={{ alignItems: "center", color: "#f43f5e" }}>
                      <ThumbUpOutlinedIcon sx={{ fontSize: 11 }} />
                      <Typography variant="caption">{p.likeCount}</Typography>
                    </Stack>
                  )}
                </Stack>
              </CardActionArea>
              <Divider />
            </Box>
          ))
        )}

        <Box sx={{ p: 1.5 }}>
          <Button
            fullWidth
            size="small"
            onClick={() => navigate(`/work/${workId}/posts`)}
            sx={{ fontWeight: 700 }}
          >
            게시판 전체 보기
          </Button>
        </Box>
      </Card>
    </Stack>
  );
}

export default PostSidebar;
