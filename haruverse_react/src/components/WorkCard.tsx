import type { ReactNode } from "react";
import { Card, CardActionArea, Box, Typography, Chip } from "@mui/material";
import StarIcon from "@mui/icons-material/Star";

// 작품 카드 — 목록에서 반복해서 쓰는 '포스터형' 프레젠테이션 컴포넌트.
// 데이터는 props로 받는다(디자인만 담당, 데이터는 부모가 넘김).
export type WorkCardProps = {
  title: string;
  season?: string;
  genres?: string[];
  imageUrl?: string | null;
  rating?: number | null;
  onClick?: () => void;
  /** 장르 칩 클릭 → 그 장르로 필터 (없으면 칩이 클릭되지 않음) */
  onGenreClick?: (genre: string) => void;
  /**
   * 포스터 좌상단에 얹을 요소 (찜 하트).
   *
   * 이 카드는 '데이터를 모르는' 프레젠테이션 컴포넌트로 두려고 한다.
   * 찜 버튼은 로그인 상태와 찜 컨텍스트를 알아야 하므로, 카드가 직접 만들지 않고
   * 부모가 완성해서 슬롯으로 꽂아 넣는다.
   */
  favoriteSlot?: ReactNode;
};

function WorkCard({ title, season, genres = [], imageUrl, rating, onClick, onGenreClick, favoriteSlot }: WorkCardProps) {
  // 카드가 좁아서 장르는 2개까지만 (나머지는 상세에서)
  const shownGenres = genres.slice(0, 2);

  return (
    <Card
      elevation={0}
      sx={{
        position: "relative", // 찜 하트를 포스터 위에 띄우기 위한 기준
        borderRadius: 2,
        overflow: "hidden",
        bgcolor: "transparent",
        transition: "transform .15s ease, box-shadow .15s ease",
        "&:hover": {
          transform: "translateY(-4px)",
          boxShadow: "0 10px 28px rgba(56,189,248,0.28)", // 시안 그림자
        },
        "&:hover .poster": { outline: "2px solid #38bdf8" }, // 시안 테두리
      }}
    >
      {/* 찜 하트 — 장르 칩과 같은 이유로 CardActionArea 밖에 둔다.
          평점 뱃지가 우상단이므로 하트는 좌상단 */}
      {favoriteSlot && (
        <Box sx={{ position: "absolute", top: 6, left: 6, zIndex: 2 }}>{favoriteSlot}</Box>
      )}

      <CardActionArea onClick={onClick}>
        {/* 세로 포스터 (2:3 표준 비율) */}
        <Box
          className="poster"
          sx={{
            position: "relative",
            aspectRatio: "2 / 3",
            bgcolor: "grey.200",
            backgroundImage: imageUrl ? `url(${imageUrl})` : undefined,
            backgroundSize: "cover",
            backgroundPosition: "center",
            borderRadius: 2,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "grey.400",
            fontSize: 13,
            transition: "outline .15s ease",
          }}
        >
          {/* 이미지가 없을 때만 자리 표시 */}
          {!imageUrl && "포스터"}

          {/* 평점 뱃지 — 포스터 우상단 */}
          {rating != null && (
            <Chip
              size="small"
              icon={<StarIcon sx={{ fontSize: 14, color: "#fbbf24 !important" }} />}
              label={rating.toFixed(1)}
              sx={{
                position: "absolute",
                top: 6,
                right: 6,
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

        {/* 메타 — 촘촘한 도감 느낌: 제목(2줄) + 회색 캡션 */}
        <Box sx={{ px: 0.5, pt: 1, pb: 0.5 }}>
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
              minHeight: 36, // 1~2줄 높이 고정 → 카드 하단 정렬 맞춤
            }}
          >
            {title}
          </Typography>
          {season && (
            <Typography
              variant="caption"
              color="text.secondary"
              noWrap
              sx={{ display: "block", mt: 0.5 }}
            >
              {season}
            </Typography>
          )}
        </Box>
      </CardActionArea>

      {/* 장르 칩 — CardActionArea 밖에 둔다.
          안에 넣으면 칩을 눌러도 카드 클릭(상세 이동)이 함께 발생한다 */}
      {shownGenres.length > 0 && (
        <Box sx={{ px: 0.5, pb: 1, display: "flex", flexWrap: "wrap", gap: 0.5 }}>
          {shownGenres.map((g) => (
            <Chip
              key={g}
              label={g}
              size="small"
              onClick={onGenreClick ? () => onGenreClick(g) : undefined}
              sx={{
                height: 20,
                fontSize: 11,
                bgcolor: "rgba(56,189,248,0.12)",
                color: "#0891b2",
                fontWeight: 600,
                cursor: onGenreClick ? "pointer" : "default",
                "& .MuiChip-label": { px: 0.8 },
                "&:hover": onGenreClick ? { bgcolor: "rgba(56,189,248,0.28)" } : undefined,
              }}
            />
          ))}
        </Box>
      )}
    </Card>
  );
}

export default WorkCard;
