import { useEffect, useState } from "react";
import { Box, Typography, Chip, Stack, Button, Alert, Skeleton, Divider, Card, CardActionArea } from "@mui/material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import MicIcon from "@mui/icons-material/Mic";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { useNavigate, useParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import { fetchCharacter } from "./api";
import { formatFavorites, ROLE_LABEL } from "./types";
import type { CharacterDetail } from "./types";

// 캐릭터 상세 — GET /api/characters/{id}
function CharacterDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // URL 파라미터 검증은 렌더 중에 계산한다.
  // (effect 안에서 곧바로 setState를 부르면 렌더가 한 번 더 돌고, react-hooks 규칙에도 걸린다)
  const characterId = Number(id);
  const invalidId = !id || !Number.isFinite(characterId);

  const [character, setCharacter] = useState<CharacterDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (invalidId) return;
    let alive = true;

    fetchCharacter(characterId)
      .then((c) => {
        if (alive) setCharacter(c);
      })
      .catch((e) => {
        if (alive) setError(e instanceof Error ? e.message : "캐릭터를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [characterId, invalidId]);

  if (invalidId) {
    return (
      <Layout>
        <Alert severity="error" sx={{ mt: 4 }}>
          잘못된 캐릭터 주소입니다.
        </Alert>
      </Layout>
    );
  }

  return (
    <Layout>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)} sx={{ mt: 2, color: "text.secondary" }}>
        뒤로
      </Button>

      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Stack direction={{ xs: "column", md: "row" }} spacing={4} sx={{ mt: 2 }}>
          <Skeleton variant="rounded" sx={{ width: 280, aspectRatio: "2 / 3", flexShrink: 0 }} />
          <Box sx={{ flex: 1 }}>
            <Skeleton width="50%" height={48} />
            <Skeleton width="30%" />
            <Skeleton variant="rounded" height={120} sx={{ mt: 3 }} />
          </Box>
        </Stack>
      ) : (
        character && (
          <Stack direction={{ xs: "column", md: "row" }} spacing={4} sx={{ mt: 2 }}>
            {/* 인물 이미지는 얼굴이 위쪽이라 backgroundPosition을 top으로 둔다 */}
            <Box
              sx={{
                width: { xs: "100%", md: 280 },
                flexShrink: 0,
                aspectRatio: "2 / 3",
                borderRadius: 3,
                bgcolor: "grey.200",
                backgroundImage: character.imageUrl ? `url(${character.imageUrl})` : undefined,
                backgroundSize: "cover",
                backgroundPosition: "top center",
              }}
            />

            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="h4" sx={{ fontWeight: 800, color: "#1b2a4a" }}>
                {character.name}
              </Typography>

              <Stack direction="row" spacing={1} sx={{ mt: 1.5, flexWrap: "wrap", gap: 1, alignItems: "center" }}>
                {character.favorites > 0 && (
                  <Chip
                    icon={<FavoriteIcon sx={{ fontSize: 16, color: "#f43f5e !important" }} />}
                    label={`${formatFavorites(character.favorites)} 즐겨찾기`}
                    size="small"
                    sx={{ bgcolor: "#0f1a2e", color: "#fff", fontWeight: 700 }}
                  />
                )}
                {/* 성우가 없는 캐릭터도 많다(단역·군중) — 있을 때만 */}
                {character.voiceActor && (
                  <Chip
                    icon={<MicIcon sx={{ fontSize: 16 }} />}
                    label={character.voiceActor}
                    size="small"
                    variant="outlined"
                    // 성우 정보가 엔티티로 연결된 경우에만 이동할 수 있다
                    onClick={
                      character.voiceActorId
                        ? () => navigate(`/voice-actor/${character.voiceActorId}`)
                        : undefined
                    }
                    sx={character.voiceActorId ? { cursor: "pointer", "&:hover": { borderColor: "#38bdf8", color: "#0891b2" } } : undefined}
                  />
                )}
              </Stack>

              <Divider sx={{ my: 3 }} />

              <Typography sx={{ fontWeight: 800, color: "#1b2a4a", mb: 1.5 }}>
                출연 작품 {character.appearances.length}편
              </Typography>

              {character.appearances.length === 0 ? (
                <Typography color="text.secondary">연결된 작품이 없습니다.</Typography>
              ) : (
                <Stack spacing={1.5}>
                  {character.appearances.map((a) => (
                    <Card
                      key={a.workId}
                      elevation={0}
                      sx={{ border: "1px solid #e5eaf2", borderRadius: 2, "&:hover": { borderColor: "#38bdf8" } }}
                    >
                      <CardActionArea onClick={() => navigate(`/work/${a.workId}`)} sx={{ p: 1.5 }}>
                        <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
                          <Box
                            sx={{
                              width: 48,
                              height: 68,
                              borderRadius: 1,
                              flexShrink: 0,
                              bgcolor: "grey.200",
                              backgroundImage: a.imageUrl ? `url(${a.imageUrl})` : undefined,
                              backgroundSize: "cover",
                              backgroundPosition: "center",
                            }}
                          />
                          <Box sx={{ minWidth: 0 }}>
                            <Typography sx={{ fontWeight: 700, color: "#1b2a4a" }}>{a.title}</Typography>
                            <Chip
                              label={ROLE_LABEL[a.role]}
                              size="small"
                              sx={{
                                mt: 0.5,
                                height: 20,
                                fontSize: 11,
                                fontWeight: 700,
                                bgcolor: a.role === "MAIN" ? "#38bdf8" : "rgba(56,189,248,0.12)",
                                color: a.role === "MAIN" ? "#fff" : "#0891b2",
                              }}
                            />
                          </Box>
                        </Stack>
                      </CardActionArea>
                    </Card>
                  ))}
                </Stack>
              )}
            </Box>
          </Stack>
        )
      )}
    </Layout>
  );
}

export default CharacterDetailPage;
