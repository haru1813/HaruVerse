import { Box, Skeleton } from "@mui/material";
import { useNavigate } from "react-router-dom";
import WorkCard from "./WorkCard";
import FavoriteButton from "./FavoriteButton";
import { workSubtitle } from "../features/work/types";
import type { Work } from "../features/work/types";

/**
 * 한 페이지에 보여줄 카드 수.
 *
 * ★아래 열 수의 공배수여야 마지막 줄이 비지 않는다★
 * 그리드는 2 / 3 / 4 / 6 / 6 열을 쓰므로 24는 전부 나누어떨어진다.
 *   24 ÷ 2 = 12줄   24 ÷ 3 = 8줄   24 ÷ 4 = 6줄   24 ÷ 6 = 4줄
 * (한때 18개 + 7열이라 마지막 줄에 4개만 남는 문제가 있었다)
 *
 * 홈과 마이페이지가 같은 값을 써야 하므로 여기서만 정의한다.
 */
export const GRID_PAGE_SIZE = 24;

export type WorkGridProps = {
  works: Work[];
  loading?: boolean;
  /** 장르 칩 클릭 처리 (마이페이지처럼 필터가 없는 화면에서는 생략) */
  onGenreClick?: (genre: string) => void;
};

/**
 * 작품 카드 그리드 — 홈과 마이페이지(찜 목록)가 공유한다.
 *
 * <p>열 수와 페이지 크기를 한 곳에 묶어두는 게 요점이다.
 * 화면마다 따로 두면 한쪽만 고쳐져서 마지막 줄이 어긋난다.
 */
function WorkGrid({ works, loading = false, onGenreClick }: WorkGridProps) {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        display: "grid",
        // 열 수는 전부 GRID_PAGE_SIZE(24)의 약수 — 마지막 줄이 항상 꽉 찬다
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
        ? // 로딩 중에는 같은 모양의 스켈레톤을 깔아 레이아웃이 튀지 않게
          Array.from({ length: GRID_PAGE_SIZE }).map((_, i) => (
            <Box key={i}>
              <Skeleton variant="rounded" sx={{ aspectRatio: "2 / 3", height: "auto" }} />
              <Skeleton width="80%" sx={{ mt: 1 }} />
              <Skeleton width="50%" />
            </Box>
          ))
        : works.map((w) => (
            <WorkCard
              key={w.id}
              title={w.title}
              season={workSubtitle(w)}
              imageUrl={w.imageUrl}
              rating={w.rating}
              genres={w.genres}
              onGenreClick={onGenreClick}
              favoriteSlot={<FavoriteButton workId={w.id} />}
              onClick={() => navigate(`/work/${w.id}`)}
            />
          ))}
    </Box>
  );
}

export default WorkGrid;
