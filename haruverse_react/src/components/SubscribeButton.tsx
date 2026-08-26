import { useState } from "react";
import { Button } from "@mui/material";
import NotificationsActiveIcon from "@mui/icons-material/NotificationsActive";
import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useSubscription } from "../contexts/SubscriptionContext";
import { UnauthorizedError } from "../lib/api";

export type SubscribeButtonProps = {
  workId: number;
  size?: "small" | "medium";
  fullWidth?: boolean;
};

/**
 * 채널 구독 버튼.
 *
 * <p><b>찜 하트와 나란히 놓이지만 뜻이 다르다</b>
 * 찜 = "이 작품이 좋다"(도감), 구독 = "이 게시판 글을 읽겠다"(커뮤니티).
 * 그래서 아이콘도 하트가 아니라 알림 벨을 쓴다 — 같은 모양이면 둘을 헷갈린다.
 *
 * <p><b>비로그인일 때 버튼을 숨기지 않는 이유</b>
 * 숨기면 "이 서비스엔 구독이 없다"고 오해한다. 대신 눌렀을 때 로그인 화면으로 보낸다.
 * (FavoriteButton과 같은 판단)
 */
function SubscribeButton({ workId, size = "small", fullWidth = false }: SubscribeButtonProps) {
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();
  const { isSubscribed, toggleSubscription } = useSubscription();

  // 연타 방지 — 서버 왕복 중에는 다시 누르지 못하게 한다
  const [busy, setBusy] = useState(false);
  const subscribed = isSubscribed(workId);

  const handleClick = async (e: React.MouseEvent) => {
    // 채널 카드 위에 얹히는 경우가 있다 — 막지 않으면 게시판으로 넘어가 버린다
    e.stopPropagation();
    e.preventDefault();

    if (!isLoggedIn) {
      navigate("/login");
      return;
    }
    if (busy) return;

    setBusy(true);
    try {
      await toggleSubscription(workId);
    } catch (err) {
      // 토큰이 만료된 사이 눌렀을 수 있다 → 로그인으로
      if (err instanceof UnauthorizedError) navigate("/login");
      // 그 밖의 실패는 Provider가 버튼을 원상복구해 주므로 별도 안내를 하지 않는다
    } finally {
      setBusy(false);
    }
  };

  return (
    <Button
      onClick={handleClick}
      disabled={busy}
      size={size}
      fullWidth={fullWidth}
      variant={subscribed ? "contained" : "outlined"}
      startIcon={subscribed ? <NotificationsActiveIcon /> : <NotificationsNoneIcon />}
      sx={{
        fontWeight: 700,
        borderRadius: 2,
        whiteSpace: "nowrap",
        ...(subscribed
          ? { bgcolor: "#0891b2", "&:hover": { bgcolor: "#0e7490" } }
          : { color: "#0891b2", borderColor: "#7dd3fc", "&:hover": { borderColor: "#0891b2" } }),
      }}
    >
      {subscribed ? "구독 중" : "구독"}
    </Button>
  );
}

export default SubscribeButton;
