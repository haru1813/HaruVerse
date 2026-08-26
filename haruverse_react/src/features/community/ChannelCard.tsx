import { Box, Typography, Chip, Stack, Card, CardActionArea, Divider } from "@mui/material";
import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import { useNavigate } from "react-router-dom";
import SubscribeButton from "../../components/SubscribeButton";
import { formatPostDate } from "./api";
import type { Channel } from "./api";

/**
 * 채널(=작품) 카드 한 장.
 *
 * <p>커뮤니티 첫 화면의 "내 구독 채널"과 "전체 채널" 두 곳에서 같은 모양을 쓴다.
 * 한쪽만 고쳐 어긋나는 일이 없도록 컴포넌트로 묶었다.
 *
 * <p>★글이 0개인 채널을 그릴 수 있어야 한다★
 * 구독은 글이 아직 없는 채널에도 걸 수 있어서 latestPost* 가 null로 온다.
 * 전체 채널 목록에서는 이런 카드가 나오지 않지만, 구독 목록에서는 나온다.
 */
function ChannelCard({ channel }: { channel: Channel }) {
  const navigate = useNavigate();
  const hasPost = channel.latestPostTitle !== null;

  return (
    <Card
      elevation={0}
      sx={{
        border: "1px solid #e5eaf2",
        borderRadius: 2,
        transition: "border-color .15s ease, box-shadow .15s ease",
        "&:hover": { borderColor: "#38bdf8", boxShadow: "0 6px 20px rgba(56,189,248,0.18)" },
      }}
    >
      {/* 카드를 누르면 그 채널의 게시판으로 — 특정 글이 아니라 채널이 단위다 */}
      <CardActionArea onClick={() => navigate(`/work/${channel.workId}/posts`)} sx={{ p: 2 }}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          {/* 작품 포스터 — 어느 채널인지 한눈에 */}
          <Box
            sx={{
              width: 44, height: 62, borderRadius: 1, flexShrink: 0,
              bgcolor: "grey.200",
              backgroundImage: channel.workImageUrl ? `url(${channel.workImageUrl})` : undefined,
              backgroundSize: "cover",
              backgroundPosition: "center",
              display: "flex", alignItems: "center", justifyContent: "center",
              color: "grey.400",
            }}
          >
            {!channel.workImageUrl && <ForumOutlinedIcon sx={{ fontSize: 20 }} />}
          </Box>

          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography
              sx={{
                fontWeight: 800, color: "#1b2a4a", lineHeight: 1.3,
                display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical",
                overflow: "hidden",
              }}
            >
              {channel.workTitle}
            </Typography>
            <Chip
              label={`글 ${channel.postCount}개`}
              size="small"
              sx={{
                mt: 0.5, height: 20, fontSize: 11, fontWeight: 700,
                bgcolor: "rgba(56,189,248,0.12)", color: "#0891b2",
              }}
            />
          </Box>

          {/* 구독 버튼 — CardActionArea 안쪽이라 클릭 전파를 막아야 한다
              (SubscribeButton이 stopPropagation 한다) */}
          <Box sx={{ flexShrink: 0 }}>
            <SubscribeButton workId={channel.workId} />
          </Box>
        </Stack>

        <Divider sx={{ my: 1.5 }} />

        {/* 최근 글 — 이 채널이 살아 있는지 보여주는 부분 */}
        {hasPost ? (
          <>
            <Typography
              variant="body2"
              sx={{
                fontWeight: 600, color: "text.primary",
                overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
              }}
            >
              {channel.latestPostTitle}
            </Typography>
            <Stack direction="row" spacing={1.5} sx={{ mt: 0.3, color: "text.secondary" }}>
              <Typography variant="caption">{channel.latestPostAuthor}</Typography>
              <Typography variant="caption">{formatPostDate(channel.latestPostCreatedAt!)}</Typography>
            </Stack>
          </>
        ) : (
          <Typography variant="body2" color="text.secondary">
            아직 글이 없어요 — 첫 글을 남겨보세요.
          </Typography>
        )}
      </CardActionArea>
    </Card>
  );
}

export default ChannelCard;
