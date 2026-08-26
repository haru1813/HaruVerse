import { Box, Skeleton } from "@mui/material";
import { useNavigate } from "react-router-dom";
import CharacterCard from "./CharacterCard";
import { GRID_PAGE_SIZE } from "./WorkGrid";
import type { CharacterSummary, CharacterRole } from "../features/character/types";

export type CharacterGridProps = {
  characters: (CharacterSummary & { role?: CharacterRole })[];
  loading?: boolean;
  /** 스켈레톤 개수 — 작품 상세처럼 한 페이지보다 적게 보여줄 때 지정 */
  skeletonCount?: number;
};

/**
 * 캐릭터 카드 그리드 — 캐릭터 도감과 작품 상세가 공유한다.
 *
 * <p>열 구성은 WorkGrid와 같은 값을 쓴다(전부 24의 약수).
 * 두 그리드가 다른 열 수를 가지면 같은 화면 안에서 카드 크기가 어긋나 보인다.
 */
function CharacterGrid({ characters, loading = false, skeletonCount = GRID_PAGE_SIZE }: CharacterGridProps) {
  const navigate = useNavigate();

  return (
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
        ? Array.from({ length: skeletonCount }).map((_, i) => (
            <Box key={i}>
              <Skeleton variant="rounded" sx={{ aspectRatio: "2 / 3", height: "auto" }} />
              <Skeleton width="80%" sx={{ mt: 1 }} />
              <Skeleton width="50%" />
            </Box>
          ))
        : characters.map((c) => (
            <CharacterCard
              key={c.id}
              name={c.name}
              imageUrl={c.imageUrl}
              favorites={c.favorites}
              voiceActor={c.voiceActor}
              role={c.role}
              onClick={() => navigate(`/character/${c.id}`)}
            />
          ))}
    </Box>
  );
}

export default CharacterGrid;
