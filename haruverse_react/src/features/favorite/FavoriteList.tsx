import { useEffect, useState } from "react";
import { Box, Typography, Pagination, Alert, Button } from "@mui/material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import { useNavigate } from "react-router-dom";
import WorkGrid, { GRID_PAGE_SIZE } from "../../components/WorkGrid";
import { useFavorite } from "../../contexts/FavoriteContext";
import { fetchMyFavorites } from "./api";
import type { Work } from "../work/types";

/**
 * 내가 찜한 작품 목록 — 마이페이지에서 사용.
 *
 * <p><b>서버 목록을 그대로 그리지 않고 favoriteIds로 한 번 거르는 이유</b>
 * 이 화면에서 하트를 눌러 찜을 풀면, 서버를 다시 조회하기 전까지 카드가 남아 있다.
 * "지웠는데 그대로네?"로 보인다. 컨텍스트의 찜 id를 기준으로 걸러내면
 * 해제하는 순간 카드가 사라지고, 다시 누르면 되돌아온다.
 */
function FavoriteList() {
  const navigate = useNavigate();
  const { favoriteIds } = useFavorite();

  const [works, setWorks] = useState<Work[]>([]);
  const [page, setPage] = useState(1); // MUI Pagination은 1부터
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;

    fetchMyFavorites(page - 1, GRID_PAGE_SIZE)
      .then((res) => {
        if (!alive) return;
        setWorks(res.content);
        setTotalPages(res.totalPages);
      })
      .catch((e) => {
        if (alive) setError(e instanceof Error ? e.message : "찜 목록을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [page]);

  // 방금 해제한 항목은 서버 재조회 없이 즉시 제외된다
  const visible = works.filter((w) => favoriteIds.has(w.id));

  return (
    <Box sx={{ mt: 2 }}>
      <Typography
        variant="h5"
        sx={{
          fontWeight: 800,
          color: "#1b2a4a",
          position: "relative",
          pl: 1.5,
          mb: 2,
          "&::before": {
            content: '""',
            position: "absolute",
            left: 0,
            top: 5,
            bottom: 5,
            width: 4,
            borderRadius: 2,
            bgcolor: "#f43f5e", // 찜 = 붉은 계열 액센트
          },
        }}
      >
        내가 찜한 작품{" "}
        <Box component="span" sx={{ color: "#f43f5e", fontSize: 20 }}>
          {favoriteIds.size}
        </Box>
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {!loading && visible.length === 0 ? (
        <Box sx={{ py: 6, textAlign: "center", border: "1px dashed #cfd8e3", borderRadius: 3 }}>
          <FavoriteIcon sx={{ fontSize: 40, color: "#e5eaf2" }} />
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            아직 찜한 작품이 없어요.
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 2 }}>
            마음에 드는 작품의 하트를 눌러보세요.
          </Typography>
          <Button variant="outlined" onClick={() => navigate("/")} sx={{ fontWeight: 700 }}>
            작품 둘러보기
          </Button>
        </Box>
      ) : (
        <WorkGrid works={visible} loading={loading} />
      )}

      {totalPages > 1 && (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
          <Pagination
            count={totalPages}
            page={page}
            onChange={(_, v) => setPage(v)}
            shape="rounded"
            variant="outlined"
          />
        </Box>
      )}
    </Box>
  );
}

export default FavoriteList;
