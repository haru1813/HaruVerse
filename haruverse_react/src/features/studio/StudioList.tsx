import { useEffect, useState } from "react";
import { Box, Typography, Pagination, Alert, Chip, Stack, Card, CardActionArea, Skeleton } from "@mui/material";
import MovieCreationOutlinedIcon from "@mui/icons-material/MovieCreationOutlined";
import { useNavigate, useSearchParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import { GRID_PAGE_SIZE } from "../../components/WorkGrid";
import { fetchStudios } from "./api";
import type { Studio } from "./api";

/**
 * 제작사 목록 — /studios
 *
 * <p><b>제작사 '상세' 화면을 따로 만들지 않은 이유</b>
 * 제작사를 고르면 결국 "그 제작사의 작품 목록"을 보고 싶은 것이다.
 * 그건 홈이 이미 하는 일이라 {@code /?studio=이름} 으로 보낸다.
 * 장르 칩을 눌렀을 때와 똑같은 흐름이라 사용자도 코드도 헷갈릴 게 없다.
 */
function StudioList() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get("q") ?? "";
  const page = Number(searchParams.get("page") ?? "1"); // MUI Pagination은 1부터

  const [studios, setStudios] = useState<Studio[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;

    (async () => {
      setLoading(true);
      setError("");
      try {
        const res = await fetchStudios({ keyword, page: page - 1, size: GRID_PAGE_SIZE });
        if (!alive) return;
        setStudios(res.content);
        setTotal(res.totalElements);
        setTotalPages(res.totalPages);
      } catch (e) {
        if (!alive) return;
        setError(e instanceof Error ? e.message : "제작사를 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [keyword, page]);

  const goToPage = (next: number) => {
    const params = new URLSearchParams(searchParams);
    if (next <= 1) params.delete("page");
    else params.set("page", String(next));
    setSearchParams(params);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const clearKeyword = () => {
    const params = new URLSearchParams(searchParams);
    params.delete("q");
    params.delete("page");
    setSearchParams(params);
  };

  return (
    <Layout>
      <Typography
        variant="h5"
        sx={{
          fontWeight: 800,
          color: "#1b2a4a",
          position: "relative",
          pl: 1.5,
          mt: 4,
          mb: 1,
          "&::before": {
            content: '""',
            position: "absolute",
            left: 0,
            top: 5,
            bottom: 5,
            width: 4,
            borderRadius: 2,
            bgcolor: "#38bdf8",
          },
        }}
      >
        {keyword ? `'${keyword}' 제작사 검색 결과` : "제작사"}
      </Typography>

      <Stack direction="row" spacing={1} sx={{ mb: 2, alignItems: "center", flexWrap: "wrap", gap: 1 }}>
        <Typography variant="body2" color="text.secondary">
          전체{" "}
          <Box component="b" sx={{ color: "#0891b2" }}>
            {total}
          </Box>
          곳 · 작품 많은 순
        </Typography>
        {keyword && (
          <Chip size="small" label={`검색: ${keyword}`} onDelete={clearKeyword} color="secondary" variant="outlined" />
        )}
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* 제작사는 이미지가 없어 텍스트 카드로 둔다.
          포스터형 그리드를 쓰면 빈 회색 상자만 잔뜩 깔린다 */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: {
            xs: "repeat(1, 1fr)",
            sm: "repeat(2, 1fr)",
            md: "repeat(3, 1fr)",
            lg: "repeat(4, 1fr)",
          },
          gap: 2,
        }}
      >
        {loading
          ? Array.from({ length: 12 }).map((_, i) => <Skeleton key={i} variant="rounded" height={88} />)
          : studios.map((s) => (
              <Card
                key={s.id}
                elevation={0}
                sx={{
                  border: "1px solid #e5eaf2",
                  borderRadius: 2,
                  transition: "border-color .15s ease, box-shadow .15s ease",
                  "&:hover": { borderColor: "#38bdf8", boxShadow: "0 6px 20px rgba(56,189,248,0.18)" },
                }}
              >
                <CardActionArea
                  // 홈의 studio 필터로 보낸다 (장르 칩과 같은 흐름)
                  onClick={() => navigate(`/?studio=${encodeURIComponent(s.name)}`)}
                  sx={{ p: 2 }}
                >
                  <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                    <Box
                      sx={{
                        width: 44,
                        height: 44,
                        borderRadius: 2,
                        flexShrink: 0,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        bgcolor: "rgba(56,189,248,0.12)",
                        color: "#0891b2",
                      }}
                    >
                      <MovieCreationOutlinedIcon />
                    </Box>
                    <Box sx={{ minWidth: 0 }}>
                      <Typography sx={{ fontWeight: 700, color: "#1b2a4a" }} noWrap>
                        {s.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        작품 {s.workCount}편
                      </Typography>
                    </Box>
                  </Stack>
                </CardActionArea>
              </Card>
            ))}
      </Box>

      {!loading && !error && studios.length === 0 && (
        <Box sx={{ py: 8, textAlign: "center" }}>
          <Typography color="text.secondary">
            {keyword ? "조건에 맞는 제작사가 없어요." : "아직 등록된 제작사가 없어요."}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {keyword ? "다른 이름으로 찾아보세요." : "작품을 수집하면 제작사도 함께 채워집니다."}
          </Typography>
        </Box>
      )}

      {totalPages > 1 && (
        <Box sx={{ width: "100%", display: "flex", justifyContent: "center", mt: 6, mb: 2 }}>
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
                bgcolor: "#38bdf8",
                borderColor: "#38bdf8",
                color: "#fff",
                "&:hover": { bgcolor: "#0ea5e9" },
              },
            }}
          />
        </Box>
      )}
    </Layout>
  );
}

export default StudioList;
