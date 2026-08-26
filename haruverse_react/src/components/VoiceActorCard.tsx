import { Box, Card, CardActionArea, Chip, Typography } from "@mui/material";
import MicIcon from "@mui/icons-material/Mic";

export type VoiceActorCardProps = {
  name: string;
  imageUrl?: string | null;
  /** 맡은 배역 수 */
  characterCount?: number;
  onClick?: () => void;
};

/**
 * 성우 카드 — 캐릭터·작품과 같은 포스터형.
 *
 * <p>처음에는 아바타가 붙은 작은 텍스트 카드였다.
 * 당시 성우 대부분이 이미지 없이 이관된 데이터라, 큰 카드로 깔면
 * 회색 상자만 보였기 때문이다. 재수집으로 사진이 채워지면서 포스터형으로 바꿨다.
 *
 * <p>사진이 없는 성우가 다시 생길 수 있으므로(아직 수집 못 한 작품의 성우)
 * 이미지가 없을 때의 표시를 남겨둔다.
 */
function VoiceActorCard({ name, imageUrl, characterCount, onClick }: VoiceActorCardProps) {
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
            // 인물 사진은 얼굴이 위쪽이라 top 기준으로 자른다
            backgroundPosition: "top center",
            borderRadius: 2,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "grey.400",
            transition: "outline .15s ease",
          }}
        >
          {/* 사진이 아직 없는 성우 */}
          {!imageUrl && <MicIcon sx={{ fontSize: 44 }} />}

          {characterCount != null && characterCount > 0 && (
            <Chip
              size="small"
              icon={<MicIcon sx={{ fontSize: 12, color: "#38bdf8 !important" }} />}
              label={`${characterCount}개 배역`}
              sx={{
                position: "absolute",
                bottom: 6,
                left: 6,
                height: 22,
                bgcolor: "rgba(15,26,46,0.82)",
                color: "#fff",
                fontWeight: 700,
                fontSize: 12,
                "& .MuiChip-label": { px: 0.6 },
              }}
            />
          )}
        </Box>

        <Box sx={{ px: 0.5, pt: 1, pb: 1 }}>
          <Typography
            sx={{
              fontWeight: 700,
              fontSize: 14,
              lineHeight: 1.3,
              color: "text.primary",
              display: "-webkit-box",
              WebkitLineClamp: 2,
              WebkitBoxOrient: "vertical",
              overflow: "hidden",
              minHeight: 36,
            }}
          >
            {name}
          </Typography>
        </Box>
      </CardActionArea>
    </Card>
  );
}

export default VoiceActorCard;
