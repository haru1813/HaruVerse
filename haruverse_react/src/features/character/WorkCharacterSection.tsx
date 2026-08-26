import { useEffect, useState } from "react";
import { Box, Typography, Button, Skeleton } from "@mui/material";
import CharacterGrid from "../../components/CharacterGrid";
import { fetchWorkCharacters } from "./api";
import type { WorkCharacter } from "./types";

/** 처음에 보여줄 인원 — 한 작품에 60명이 넘는 경우가 많아 전부 깔면 상세가 캐릭터로 뒤덮인다 */
const PREVIEW_COUNT = 12;

/**
 * 작품 상세의 등장인물 섹션.
 *
 * <p><b>작품 본문과 따로 불러오는 이유</b>
 * 작품 상세 API에 캐릭터를 끼워 넣으면 캐릭터가 없는 게임까지 매번 조인 비용을 물고,
 * 응답도 무거워진다(한 작품에 60건이 넘는다).
 * 따로 부르면 캐릭터가 없는 작품에서는 이 섹션이 조용히 사라진다.
 */
function WorkCharacterSection({ workId }: { workId: number }) {
  const [characters, setCharacters] = useState<WorkCharacter[]>([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    let alive = true;

    fetchWorkCharacters(workId)
      .then((list) => {
        if (alive) setCharacters(list);
      })
      .catch(() => {
        // 캐릭터를 못 불러와도 작품 상세는 보여야 한다
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [workId]);

  if (loading) {
    return (
      <Box sx={{ mt: 2 }}>
        <Skeleton width={140} height={28} sx={{ mb: 1.5 }} />
        <CharacterGrid characters={[]} loading skeletonCount={6} />
      </Box>
    );
  }

  // 캐릭터 정보가 없는 작품(게임 등)에서는 섹션 자체를 그리지 않는다
  if (characters.length === 0) return null;

  const shown = expanded ? characters : characters.slice(0, PREVIEW_COUNT);
  const hidden = characters.length - shown.length;

  return (
    <Box sx={{ mt: 2 }}>
      <Typography sx={{ fontWeight: 800, color: "#1b2a4a", mb: 1.5 }}>
        등장인물{" "}
        <Box component="span" sx={{ color: "#0891b2" }}>
          {characters.length}명
        </Box>
      </Typography>

      <CharacterGrid characters={shown} />

      {hidden > 0 && (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 2 }}>
          <Button variant="outlined" onClick={() => setExpanded(true)} sx={{ fontWeight: 700 }}>
            나머지 {hidden}명 더 보기
          </Button>
        </Box>
      )}
    </Box>
  );
}

export default WorkCharacterSection;
