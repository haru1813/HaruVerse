import { useEffect, useState } from "react";
import { Box, Typography, Pagination, Alert, Chip, Stack } from "@mui/material";
import { useSearchParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import WorkGrid, { GRID_PAGE_SIZE } from "../../components/WorkGrid";
import { fetchWorks } from "../work/api";
import type { Work, WorkType } from "../work/types";
import hero from "../../assets/hero.png";

// 페이지당 카드 수 — 그리드 열 수와의 정합은 WorkGrid가 책임진다
const PAGE_SIZE = GRID_PAGE_SIZE;

const TYPE_LABEL: Record<string, string> = { ANIME: "애니메이션", GAME: "게임" };

function Home() {
  // 검색·필터·페이지는 전부 URL에서 읽는다 (헤더의 검색바·탭이 URL을 바꾼다)
  const [searchParams, setSearchParams] = useSearchParams();
  const type = (searchParams.get("type") ?? undefined) as WorkType | undefined;
  const keyword = searchParams.get("q") ?? "";
  const genre = searchParams.get("genre") ?? undefined;
  const studio = searchParams.get("studio") ?? undefined;
  const page = Number(searchParams.get("page") ?? "1"); // MUI Pagination은 1부터

  const [works, setWorks] = useState<Work[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true; // 응답이 늦게 왔는데 화면이 사라진 경우 setState 방지

    (async () => {
      setLoading(true);
      setError("");
      try {
        // MUI는 1-based, 백엔드 Pageable은 0-based → 여기서 변환
        const res = await fetchWorks({ type, genre, studio, keyword, page: page - 1, size: PAGE_SIZE });
        if (!alive) return;
        setWorks(res.content);
        setTotal(res.totalElements);
        setTotalPages(res.totalPages);
      } catch (e) {
        if (!alive) return;
        setError(e instanceof Error ? e.message : "작품을 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [type, genre, studio, keyword, page]);

  /** 페이지 이동 — 기존 필터는 유지하고 page만 갈아끼운다 */
  const goToPage = (next: number) => {
    const params = new URLSearchParams(searchParams);
    if (next <= 1) params.delete("page"); // 1페이지는 URL을 깔끔하게
    else params.set("page", String(next));
    setSearchParams(params);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  /** 필터 칩 제거 */
  const clearFilter = (key: "type" | "q" | "genre" | "studio") => {
    const params = new URLSearchParams(searchParams);
    params.delete(key);
    params.delete("page"); // 조건이 바뀌면 1페이지부터
    setSearchParams(params);
  };

  const hasFilter = Boolean(type || genre || studio || keyword);

  /** 카드의 장르 칩을 누르면 그 장르로 필터 */
  const filterByGenre = (name: string) => {
    const params = new URLSearchParams(searchParams);
    params.set("genre", name);
    params.delete("page");
    setSearchParams(params);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <Layout>
      {/* 히어로 배너 — HaruVerse 브랜드 */}
      <Box
        sx={{
          position: "relative",
          borderRadius: 4,
          overflow: "hidden",
          height: { xs: 200, md: 300 },
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          px: { xs: 3, md: 6 },
          color: "#fff",
          backgroundImage: `linear-gradient(135deg, rgba(27,42,74,0.88), rgba(37,99,235,0.82)), url(${hero})`,
          backgroundSize: "cover",
          backgroundPosition: "center",
        }}
      >
        <Typography variant="h3" sx={{ fontWeight: 800, letterSpacing: "-1px" }}>
          Haru<span style={{ color: "#38bdf8" }}>Verse</span>
        </Typography>
        <Typography variant="h6" sx={{ opacity: 0.95, mt: 1 }}>
          애니메이션 · 게임 통합 도감
        </Typography>
      </Box>

      {/* 섹션 헤더 — 제목(시안 바) */}
      <Typography
        variant="h5"
        sx={{
          fontWeight: 800,
          color: "#1b2a4a",
          position: "relative",
          pl: 1.5,
          mt: 6,
          mb: 1,
          "&::before": {
            content: '""',
            position: "absolute",
            left: 0,
            top: 5,
            bottom: 5,
            width: 4,
            borderRadius: 2,
            bgcolor: "#38bdf8", // 시안 액센트 바
          },
        }}
      >
        {keyword
          ? `'${keyword}' 검색 결과`
          : studio
            ? `${studio} 작품`
            : genre
              ? `${genre} 장르`
              : type
                ? TYPE_LABEL[type]
                : "작품 도감"}
      </Typography>

      {/* 적용된 필터를 칩으로 보여주고, X로 개별 해제 */}
      <Stack direction="row" spacing={1} sx={{ mb: 2, alignItems: "center", flexWrap: "wrap", gap: 1 }}>
        <Typography variant="body2" color="text.secondary">
          전체 <Box component="b" sx={{ color: "#0891b2" }}>{total}</Box>개
        </Typography>
        {type && (
          <Chip size="small" label={TYPE_LABEL[type]} onDelete={() => clearFilter("type")} color="primary" variant="outlined" />
        )}
        {genre && (
          <Chip size="small" label={genre} onDelete={() => clearFilter("genre")} color="info" variant="outlined" />
        )}
        {studio && (
          <Chip size="small" label={`제작사: ${studio}`} onDelete={() => clearFilter("studio")} color="info" variant="outlined" />
        )}
        {keyword && (
          <Chip size="small" label={`검색: ${keyword}`} onDelete={() => clearFilter("q")} color="secondary" variant="outlined" />
        )}
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* 카드 그리드 — 열 수와 페이지 크기의 정합은 WorkGrid가 관리한다 */}
      <WorkGrid works={works} loading={loading} onGenreClick={filterByGenre} />

      {/* 데이터가 아예 없을 때 안내 */}
      {!loading && !error && works.length === 0 && (
        <Box sx={{ py: 8, textAlign: "center" }}>
          <Typography color="text.secondary">
            {hasFilter ? "조건에 맞는 작품이 없어요." : "아직 등록된 작품이 없어요."}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {hasFilter
              ? "검색어나 카테고리를 바꿔보세요."
              : /* TODO(하루): 관리자 화면이 생기면 이 안내 대신 수집 버튼을 둔다 */
                "수집 API(POST /api/collect/jikan/ids)로 데이터를 채워보세요."}
          </Typography>
        </Box>
      )}

      {/* 페이지네이션 — 서버 총 페이지 수에 맞춰 렌더 */}
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
              "& .MuiPagination-ul": { justifyContent: "center" },
              "& .MuiPaginationItem-root": {
                border: "1px solid #cfd8e3",
                fontSize: 16,
                minWidth: 42,
                height: 42,
              },
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

export default Home;
