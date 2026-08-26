import { useEffect, useState } from "react";
import { Box, Typography, Chip, Stack, Button, Alert, Skeleton, Divider } from "@mui/material";
import MicIcon from "@mui/icons-material/Mic";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { useNavigate, useParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import CharacterGrid from "../../components/CharacterGrid";
import { fetchVoiceActor } from "./api";
import type { VoiceActorDetail } from "./api";

// 성우 상세 — GET /api/voice-actors/{id}
function VoiceActorDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // URL 파라미터 검증은 렌더 중에 계산한다 (effect에서 setState 하면 렌더가 한 번 더 돈다)
  const voiceActorId = Number(id);
  const invalidId = !id || !Number.isFinite(voiceActorId);

  const [voiceActor, setVoiceActor] = useState<VoiceActorDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (invalidId) return;
    let alive = true;

    fetchVoiceActor(voiceActorId)
      .then((v) => {
        if (alive) setVoiceActor(v);
      })
      .catch((e) => {
        if (alive) setError(e instanceof Error ? e.message : "성우를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [voiceActorId, invalidId]);

  if (invalidId) {
    return (
      <Layout>
        <Alert severity="error" sx={{ mt: 4 }}>
          잘못된 성우 주소입니다.
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
            <Skeleton width="40%" height={48} />
            <Skeleton width="20%" />
            <Skeleton variant="rounded" height={220} sx={{ mt: 3 }} />
          </Box>
        </Stack>
      ) : (
        voiceActor && (
          <Stack direction={{ xs: "column", md: "row" }} spacing={4} sx={{ mt: 2 }}>
            {/* 캐릭터 상세와 같은 크기의 세로 이미지 — 인물이라 top 기준으로 자른다 */}
            <Box
              sx={{
                width: { xs: "100%", md: 280 },
                flexShrink: 0,
                aspectRatio: "2 / 3",
                borderRadius: 3,
                bgcolor: "grey.200",
                backgroundImage: voiceActor.imageUrl ? `url(${voiceActor.imageUrl})` : undefined,
                backgroundSize: "cover",
                backgroundPosition: "top center",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "grey.400",
              }}
            >
              {/* 사진이 아직 없는 성우 */}
              {!voiceActor.imageUrl && <MicIcon sx={{ fontSize: 64 }} />}
            </Box>

            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="h4" sx={{ fontWeight: 800, color: "#1b2a4a" }}>
                {voiceActor.name}
              </Typography>
              <Chip
                icon={<MicIcon sx={{ fontSize: 16 }} />}
                label={`${voiceActor.characters.length}개 배역`}
                size="small"
                sx={{ mt: 1.5, bgcolor: "#0f1a2e", color: "#fff", fontWeight: 700 }}
              />

              <Divider sx={{ my: 3 }} />

              <Typography sx={{ fontWeight: 800, color: "#1b2a4a", mb: 1.5 }}>맡은 캐릭터</Typography>

              {voiceActor.characters.length === 0 ? (
                <Typography color="text.secondary">연결된 캐릭터가 없습니다.</Typography>
              ) : (
                <CharacterGrid characters={voiceActor.characters} />
              )}
            </Box>
          </Stack>
        )
      )}
    </Layout>
  );
}

export default VoiceActorDetailPage;
