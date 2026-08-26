import { useState } from "react";
import { IconButton, Button, Tooltip } from "@mui/material";
import FavoriteIcon from "@mui/icons-material/Favorite";
import FavoriteBorderIcon from "@mui/icons-material/FavoriteBorder";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useFavorite } from "../contexts/FavoriteContext";
import { UnauthorizedError } from "../lib/api";

export type FavoriteButtonProps = {
  workId: number;
  /** icon = 카드 위 작은 하트 / labeled = 상세 페이지의 글자 있는 버튼 */
  variant?: "icon" | "labeled";
};

/**
 * 찜 버튼.
 *
 * <p><b>비로그인일 때 버튼을 숨기지 않는 이유</b>
 * 숨기면 "이 서비스엔 찜이 없다"고 오해한다. 대신 눌렀을 때 로그인 화면으로 보낸다.
 *
 * <p><b>클릭 전파를 막는 이유</b>
 * 카드 위에 얹히므로, 막지 않으면 하트를 눌렀는데 상세 페이지로 넘어가 버린다.
 */
function FavoriteButton({ workId, variant = "icon" }: FavoriteButtonProps) {
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();
  const { isFavorite, toggleFavorite } = useFavorite();

  // 연타 방지 — 서버 왕복 중에는 다시 누르지 못하게 한다
  const [busy, setBusy] = useState(false);
  const favorited = isFavorite(workId);

  const handleClick = async (e: React.MouseEvent) => {
    e.stopPropagation(); // 카드 클릭(상세 이동)으로 새지 않게
    e.preventDefault();

    if (!isLoggedIn) {
      navigate("/login");
      return;
    }
    if (busy) return;

    setBusy(true);
    try {
      await toggleFavorite(workId);
    } catch (err) {
      // 토큰이 만료된 사이 눌렀을 수 있다 → 로그인으로
      if (err instanceof UnauthorizedError) navigate("/login");
      // 그 밖의 실패는 Provider가 하트를 원상복구해 주므로 별도 안내를 하지 않는다
    } finally {
      setBusy(false);
    }
  };

  const label = favorited ? "찜 해제" : "찜하기";

  if (variant === "labeled") {
    return (
      <Button
        onClick={handleClick}
        disabled={busy}
        variant={favorited ? "contained" : "outlined"}
        color="error"
        startIcon={favorited ? <FavoriteIcon /> : <FavoriteBorderIcon />}
        sx={{ fontWeight: 700, borderRadius: 2 }}
      >
        {label}
      </Button>
    );
  }

  return (
    <Tooltip title={label} enterDelay={400}>
      {/* disabled 버튼은 이벤트를 안 받아 Tooltip이 깨지므로 span으로 감싼다 */}
      <span>
        <IconButton
          onClick={handleClick}
          disabled={busy}
          size="small"
          aria-label={label}
          sx={{
            bgcolor: "rgba(15,26,46,0.82)",
            color: favorited ? "#f43f5e" : "rgba(255,255,255,0.9)",
            "&:hover": { bgcolor: "rgba(15,26,46,0.95)", color: "#f43f5e" },
            "&.Mui-disabled": { bgcolor: "rgba(15,26,46,0.5)" },
          }}
        >
          {favorited ? <FavoriteIcon sx={{ fontSize: 16 }} /> : <FavoriteBorderIcon sx={{ fontSize: 16 }} />}
        </IconButton>
      </span>
    </Tooltip>
  );
}

export default FavoriteButton;
