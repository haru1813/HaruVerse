import { useEffect, useState } from "react";
import { Box, Typography, Chip, Stack, Button, Alert, Skeleton, Divider } from "@mui/material";
import StarIcon from "@mui/icons-material/Star";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import { useNavigate, useParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import FavoriteButton from "../../components/FavoriteButton";
import WorkCharacterSection from "../character/WorkCharacterSection";
import { fetchWork } from "./api";
import { formatSeason, sortPlatforms } from "./types";
import type { WorkDetail } from "./types";

// 작품 상세 — GET /api/works/{id}
function WorkDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // URL 파라미터 검증은 렌더 중에 계산한다.
  // (effect 안에서 곧바로 setState를 부르면 렌더 → 상태변경 → 재렌더가 한 번 더 돌아
  //  불필요하고, react-hooks 규칙에도 걸린다)
  const workId = Number(id);
  const invalidId = !id || !Number.isFinite(workId);

  const [work, setWork] = useState<WorkDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(!invalidId); // 잘못된 id면 로딩 자체가 없음

  // 게임은 RAWG가 가로 이미지를 준다 → 포스터 틀을 다르게 잡는다 (아래 sx 참고)
  const isGame = work?.type === "GAME";

  useEffect(() => {
    if (invalidId) return;

    let alive = true;
    (async () => {
      setLoading(true);
      setError("");
      try {
        const data = await fetchWork(workId);
        if (alive) setWork(data);
      } catch (e) {
        if (alive) setError(e instanceof Error ? e.message : "작품을 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [workId, invalidId]);

  return (
    <Layout>
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate(-1)}
        sx={{ mb: 2, color: "text.secondary", fontWeight: 700 }}
      >
        뒤로
      </Button>

      {invalidId && <Alert severity="error">잘못된 작품 번호입니다.</Alert>}
      {error && <Alert severity="error">{error}</Alert>}

      {loading ? (
        <Stack direction={{ xs: "column", md: "row" }} spacing={4}>
          <Skeleton variant="rounded" sx={{ width: { xs: "100%", md: 300 }, aspectRatio: "2 / 3" }} />
          <Box sx={{ flex: 1 }}>
            <Skeleton width="50%" height={44} />
            <Skeleton width="30%" />
            <Skeleton variant="rounded" height={160} sx={{ mt: 3 }} />
          </Box>
        </Stack>
      ) : (
        work && (
          <Stack direction={{ xs: "column", md: "row" }} spacing={4}>
            {/* 포스터 */}
            <Box
              sx={{
                // ★작품 종류에 따라 비율이 다르다★
                //   애니는 세로 포스터(2:3), 게임은 RAWG가 가로 이미지를 준다(대개 16:9).
                //   가로 이미지를 세로 틀에 cover하면 크게 확대되며 좌우가 잘린다.
                //   해상도가 큰 게임은 그럭저럭 버티지만, 작은 이미지(스타레일 552x414)는
                //   흐릿하게 늘어나 눈에 띈다.
                width: { xs: "100%", md: isGame ? 480 : 300 },
                // ★alignSelf 필수★ Stack(row)의 기본값은 alignItems:stretch라
                //   이 박스가 오른쪽 본문 높이만큼 세로로 늘어난다.
                //   그러면 aspectRatio가 무시된다(높이가 auto일 때만 동작).
                //   스타레일처럼 본문이 긴 작품에서 포스터가 2500px까지 늘어났다.
                alignSelf: "flex-start",
                flexShrink: 0,
                aspectRatio: isGame ? "16 / 9" : "2 / 3",
                borderRadius: 3,
                bgcolor: "grey.200",
                backgroundImage: work.imageUrl ? `url(${work.imageUrl})` : undefined,
                backgroundSize: "cover",
                backgroundPosition: "center",
                boxShadow: "0 8px 30px rgba(27,42,74,0.18)",
              }}
            />

            {/* 정보 */}
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="h4" sx={{ fontWeight: 800, color: "#1b2a4a", wordBreak: "keep-all" }}>
                {work.title}
              </Typography>

              <Stack direction="row" spacing={1} sx={{ mt: 2, flexWrap: "wrap", gap: 1 }}>
                <Chip label={work.type === "ANIME" ? "애니메이션" : "게임"} color="primary" size="small" sx={{ fontWeight: 700 }} />
                {work.season && (
                  <Chip label={formatSeason(work.season)} size="small" variant="outlined" />
                )}
                {work.releaseDate && (
                  <Chip label={work.releaseDate} size="small" variant="outlined" />
                )}
                {work.rating != null && (
                  <Chip
                    icon={<StarIcon sx={{ fontSize: 16, color: "#fbbf24 !important" }} />}
                    label={work.rating.toFixed(1)}
                    size="small"
                    sx={{ bgcolor: "#0f1a2e", color: "#fff", fontWeight: 700 }}
                  />
                )}
              </Stack>

              {/* 제작사 */}
              {work.studio && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                  제작사{" "}
                  {/* 장르 칩과 같은 흐름 — 누르면 그 제작사의 작품 목록으로 */}
                  <Box
                    component="span"
                    role="link"
                    tabIndex={0}
                    onClick={() => navigate(`/?studio=${encodeURIComponent(work.studio!)}`)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") navigate(`/?studio=${encodeURIComponent(work.studio!)}`);
                    }}
                    sx={{
                      color: "#0891b2",
                      fontWeight: 700,
                      cursor: "pointer",
                      "&:hover": { textDecoration: "underline" },
                    }}
                  >
                    {work.studio}
                  </Box>
                </Typography>
              )}

              {/* 장르 — 클릭하면 그 장르 목록으로 이동 */}
              {work.genres.length > 0 && (
                <Stack direction="row" spacing={1} sx={{ mt: 1.5, flexWrap: "wrap", gap: 1 }}>
                  {work.genres.map((g) => (
                    <Chip
                      key={g}
                      label={g}
                      size="small"
                      onClick={() => navigate(`/?genre=${encodeURIComponent(g)}`)}
                      sx={{
                        bgcolor: "rgba(56,189,248,0.12)",
                        color: "#0891b2",
                        fontWeight: 700,
                        cursor: "pointer",
                        "&:hover": { bgcolor: "rgba(56,189,248,0.28)" },
                      }}
                    />
                  ))}
                </Stack>
              )}

              {/* 플랫폼 — 게임에만 값이 있다.
                  ★장르와 달리 누를 수 없다★ 플랫폼별 목록이 아직 없어서,
                  칩 모양이 같으면 눌러도 아무 일이 없어 고장으로 보인다. 그래서 외곽선 회색 칩. */}
              {work.platforms.length > 0 && (
                <Stack direction="row" spacing={1} sx={{ mt: 1.5, flexWrap: "wrap", gap: 1, alignItems: "center" }}>
                  <Typography variant="caption" sx={{ color: "text.secondary", fontWeight: 700 }}>
                    플랫폼
                  </Typography>
                  {sortPlatforms(work.platforms).map((p) => (
                    <Chip
                      key={p}
                      label={p}
                      size="small"
                      variant="outlined"
                      sx={{ fontWeight: 700, color: "#475569", borderColor: "#cbd5e1" }}
                    />
                  ))}
                </Stack>
              )}

              {/* 찜하기 · 게시판 — 비로그인이면 누를 때 로그인 화면으로 보낸다 */}
              <Stack direction="row" spacing={1.5} sx={{ mt: 3, flexWrap: "wrap", gap: 1.5 }}>
                <FavoriteButton workId={workId} variant="labeled" />
                <Button
                  variant="outlined"
                  startIcon={<ForumOutlinedIcon />}
                  onClick={() => navigate(`/work/${workId}/posts`)}
                  sx={{ fontWeight: 700, borderRadius: 2 }}
                >
                  게시판
                </Button>
              </Stack>

              <Divider sx={{ my: 3 }} />

              <Typography sx={{ fontWeight: 800, color: "#1b2a4a", mb: 1 }}>줄거리</Typography>
              <Typography sx={{ color: "text.secondary", lineHeight: 1.9, whiteSpace: "pre-line" }}>
                {work.synopsis ?? "등록된 줄거리가 없습니다."}
              </Typography>

              <Divider sx={{ my: 3 }} />

              {/* 등장인물 — 캐릭터가 없는 작품(게임)에서는 아무것도 그리지 않는다 */}
              <WorkCharacterSection workId={workId} />

              <Divider sx={{ my: 3 }} />

              <Typography variant="caption" color="text.secondary">
                데이터 출처: {work.source} · {work.externalId}
              </Typography>

            </Box>
          </Stack>
        )
      )}
    </Layout>
  );
}

export default WorkDetailPage;
