import { useEffect, useState } from "react";
import { Box, Typography, Pagination, Alert, Chip, Stack } from "@mui/material";
import { useSearchParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import CharacterGrid from "../../components/CharacterGrid";
import { GRID_PAGE_SIZE } from "../../components/WorkGrid";
import { fetchCharacters } from "./api";
import type { CharacterSummary } from "./types";

/**
 * 캐릭터 도감 — /characters
 *
 * <p>홈과 같은 방식으로 <b>URL을 상태의 원천</b>으로 삼는다.
 * 헤더 검색창이 이 경로에서는 /characters?q=... 로 보내준다.
 */
function CharacterList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get("q") ?? "";
  const page = Number(searchParams.get("page") ?? "1"); // MUI Pagination은 1부터

  const [characters, setCharacters] = useState<CharacterSummary[]>([]);
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
        const res = await fetchCharacters({ keyword, page: page - 1, size: GRID_PAGE_SIZE });
        if (!alive) return;
        setCharacters(res.content);
        setTotal(res.totalElements);
        setTotalPages(res.totalPages);
      } catch (e) {
        if (!alive) return;
        setError(e instanceof Error ? e.message : "캐릭터를 불러오지 못했습니다.");
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
          fontWeight: 800, color: "#1b2a4a", position: "relative", pl: 1.5, mt: 4, mb: 1,
          "&::before": {
            content: '""', position: "absolute", left: 0, top: 5, bottom: 5,
            width: 4, borderRadius: 2, bgcolor: "#38bdf8",
          },
        }}
      >
        {keyword ? `'${keyword}' 캐릭터 검색 결과` : "캐릭터 도감"}
      </Typography>

      <Stack direction="row" spacing={1} sx={{ mb: 2, alignItems: "center", flexWrap: "wrap", gap: 1 }}>
        <Typography variant="body2" color="text.secondary">
          전체 <Box component="b" sx={{ color: "#0891b2" }}>{total}</Box>명 · 인기순
        </Typography>
        {keyword && (
          <Chip size="small" label={`검색: ${keyword}`} onDelete={clearKeyword} color="secondary" variant="outlined" />
        )}
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <CharacterGrid characters={characters} loading={loading} />

      {!loading && !error && characters.length === 0 && (
        <Box sx={{ py: 8, textAlign: "center" }}>
          <Typography color="text.secondary">
            {keyword ? "조건에 맞는 캐릭터가 없어요." : "아직 등록된 캐릭터가 없어요."}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {keyword
              ? "다른 이름으로 찾아보세요."
              : /* TODO(하루): 관리자 화면이 생기면 이 안내 대신 수집 버튼을 둔다 */
                "수집 API(POST /api/collect/jikan/characters)로 데이터를 채워보세요."}
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

export default CharacterList;
