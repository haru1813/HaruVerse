import { useEffect, useState } from "react";
import { Box, Typography, Pagination, Alert, Chip, Stack, Skeleton } from "@mui/material";
import { useNavigate, useSearchParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import VoiceActorCard from "../../components/VoiceActorCard";
import { GRID_PAGE_SIZE } from "../../components/WorkGrid";
import { fetchVoiceActors } from "./api";
import type { VoiceActorSummary } from "./api";

/**
 * 성우 도감 — /voice-actors
 *
 * <p>캐릭터·작품과 같은 포스터형 그리드를 쓴다.
 * (한때는 이미지 없이 이관된 데이터뿐이라 작은 텍스트 카드였다 —
 *  재수집으로 사진이 채워지면서 바꿨다)
 */
function VoiceActorList() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get("q") ?? "";
  const page = Number(searchParams.get("page") ?? "1"); // MUI Pagination은 1부터

  const [voiceActors, setVoiceActors] = useState<VoiceActorSummary[]>([]);
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
        const res = await fetchVoiceActors({ keyword, page: page - 1, size: GRID_PAGE_SIZE });
        if (!alive) return;
        setVoiceActors(res.content);
        setTotal(res.totalElements);
        setTotalPages(res.totalPages);
      } catch (e) {
        if (!alive) return;
        setError(e instanceof Error ? e.message : "성우를 불러오지 못했습니다.");
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
        {keyword ? `'${keyword}' 성우 검색 결과` : "성우"}
      </Typography>

      <Stack direction="row" spacing={1} sx={{ mb: 2, alignItems: "center", flexWrap: "wrap", gap: 1 }}>
        <Typography variant="body2" color="text.secondary">
          전체{" "}
          <Box component="b" sx={{ color: "#0891b2" }}>
            {total}
          </Box>
          명 · 맡은 배역 많은 순
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

      {/* 열 구성은 WorkGrid·CharacterGrid와 같은 값 —
          같은 화면 안에서 카드 크기가 어긋나 보이지 않게 */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: {
            xs: "repeat(2, 1fr)",
            sm: "repeat(3, 1fr)",
            md: "repeat(4, 1fr)",
            lg: "repeat(6, 1fr)",
            xl: "repeat(6, 1fr)",
          },
          gap: 2,
        }}
      >
        {loading
          ? Array.from({ length: GRID_PAGE_SIZE }).map((_, i) => (
              <Box key={i}>
                <Skeleton variant="rounded" sx={{ aspectRatio: "2 / 3", height: "auto" }} />
                <Skeleton width="80%" sx={{ mt: 1 }} />
              </Box>
            ))
          : voiceActors.map((v) => (
              <VoiceActorCard
                key={v.id}
                name={v.name}
                imageUrl={v.imageUrl}
                characterCount={v.characterCount}
                onClick={() => navigate(`/voice-actor/${v.id}`)}
              />
            ))}
      </Box>

      {!loading && !error && voiceActors.length === 0 && (
        <Box sx={{ py: 8, textAlign: "center" }}>
          <Typography color="text.secondary">
            {keyword ? "조건에 맞는 성우가 없어요." : "아직 등록된 성우가 없어요."}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {keyword ? "다른 이름으로 찾아보세요." : "캐릭터를 수집하면 성우도 함께 채워집니다."}
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

export default VoiceActorList;
