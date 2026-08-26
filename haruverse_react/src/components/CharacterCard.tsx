import { Box, Card, CardActionArea, Chip, Typography } from "@mui/material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import MicIcon from "@mui/icons-material/Mic";
import { formatFavorites, ROLE_LABEL } from "../features/character/types";
import type { CharacterRole } from "../features/character/types";

export type CharacterCardProps = {
  name: string;
  imageUrl?: string | null;
  favorites?: number;
  voiceActor?: string | null;
  /** 작품 상세에서만 표시 (목록에서는 역할이 의미 없다 — 작품마다 다르므로) */
  role?: CharacterRole;
  onClick?: () => void;
};

/**
 * 캐릭터 카드.
 *
 * <p>WorkCard와 모양은 비슷하지만 담는 정보가 다르다 —
 * 찜·장르·평점 대신 <b>성우</b>와 <b>인기</b>가 들어간다.
 * 억지로 한 컴포넌트로 합치면 안 쓰는 props가 잔뜩 생기므로 나눴다.
 */
function CharacterCard({ name, imageUrl, favorites, voiceActor, role, onClick }: CharacterCardProps) {
  return (
    <Card
      elevation={0}
      sx={{
        borderRadius: 2,
        overflow: "hidden",
        bgcolor: "transparent",
        transition: "transform .15s ease, box-shadow .15s ease",
        "&:hover": {
          transform: "translateY(-4px)",
          boxShadow: "0 10px 28px rgba(56,189,248,0.28)",
        },
        "&:hover .poster": { outline: "2px solid #38bdf8" },
      }}
    >
      <CardActionArea onClick={onClick}>
        <Box
          className="poster"
          sx={{
            position: "relative",
            aspectRatio: "2 / 3",
            bgcolor: "grey.200",
            backgroundImage: imageUrl ? `url(${imageUrl})` : undefined,
            backgroundSize: "cover",
            backgroundPosition: "top center", // 인물은 얼굴이 위쪽이라 top 기준
            borderRadius: 2,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "grey.400",
            fontSize: 13,
            transition: "outline .15s ease",
          }}
        >
          {!imageUrl && "이미지 없음"}

          {/* 주역 표시 — 조역은 수가 많아 표시하지 않는다(시각적 소음) */}
          {role === "MAIN" && (
            <Chip
              label={ROLE_LABEL.MAIN}
              size="small"
              sx={{
                position: "absolute", top: 6, left: 6, height: 22,
                bgcolor: "#38bdf8", color: "#fff", fontWeight: 700, fontSize: 11,
                "& .MuiChip-label": { px: 0.8 },
              }}
            />
          )}

          {/* 인기 — 포스터 우상단 */}
          {favorites != null && favorites > 0 && (
            <Chip
              size="small"
              icon={<FavoriteIcon sx={{ fontSize: 12, color: "#f43f5e !important" }} />}
              label={formatFavorites(favorites)}
              sx={{
                position: "absolute", top: 6, right: 6, height: 22,
                bgcolor: "rgba(15,26,46,0.82)", color: "#fff", fontWeight: 700, fontSize: 12,
                "& .MuiChip-label": { px: 0.6 },
              }}
            />
          )}
        </Box>

        <Box sx={{ px: 0.5, pt: 1, pb: 1 }}>
          <Typography
            sx={{
              fontWeight: 700, fontSize: 14, lineHeight: 1.3, color: "text.primary",
              display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical",
              overflow: "hidden", minHeight: 36,
            }}
          >
            {name}
          </Typography>

          {/* 성우 — 없는 캐릭터도 많다(단역·군중). 있을 때만 표시 */}
          {voiceActor && (
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.4, mt: 0.5, minWidth: 0 }}>
              <MicIcon sx={{ fontSize: 12, color: "text.secondary", flexShrink: 0 }} />
              <Typography variant="caption" color="text.secondary" noWrap>
                {voiceActor}
              </Typography>
            </Box>
          )}
        </Box>
      </CardActionArea>
    </Card>
  );
}

export default CharacterCard;
