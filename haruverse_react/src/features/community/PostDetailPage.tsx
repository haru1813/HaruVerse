import { useEffect, useState } from "react";
import {
  Box, Typography, Button, Stack, Alert, Divider, Card, Skeleton,
  TextField, IconButton,
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import ThumbUpIcon from "@mui/icons-material/ThumbUp";
import ThumbUpOutlinedIcon from "@mui/icons-material/ThumbUpOutlined";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import EditIcon from "@mui/icons-material/Edit";
import { useNavigate, useParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import { useAuth } from "../../contexts/AuthContext";
import { UnauthorizedError } from "../../lib/api";
import {
  createComment, deleteComment, deletePost, fetchComments, fetchPost,
  formatPostDate, likePost, unlikePost,
} from "./api";
import type { Comment, PostDetail } from "./api";
import PostSidebar from "./PostSidebar";

/** 글 상세 — /post/:id */
function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();

  const postId = Number(id);
  const invalidId = !id || !Number.isFinite(postId);

  const [post, setPost] = useState<PostDetail | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentText, setCommentText] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (invalidId) return;
    let alive = true;

    Promise.all([fetchPost(postId), fetchComments(postId)])
      .then(([p, c]) => {
        if (!alive) return;
        setPost(p);
        setComments(c);
      })
      .catch((e) => {
        if (alive) setError(e instanceof Error ? e.message : "글을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [postId, invalidId]);

  if (invalidId) {
    return (
      <Layout>
        <Alert severity="error" sx={{ mt: 4 }}>잘못된 글 주소입니다.</Alert>
      </Layout>
    );
  }

  /**
   * 추천 토글 — 찜과 같은 낙관적 갱신.
   * 화면을 먼저 바꾸고 요청을 보낸 뒤, 실패하면 되돌린다.
   */
  const toggleLike = async () => {
    if (!post) return;
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }
    const was = post.likedByMe;
    setPost({ ...post, likedByMe: !was, likeCount: post.likeCount + (was ? -1 : 1) });
    try {
      await (was ? unlikePost(postId) : likePost(postId));
    } catch (e) {
      setPost({ ...post, likedByMe: was, likeCount: post.likeCount }); // 롤백
      if (e instanceof UnauthorizedError) navigate("/login");
    }
  };

  const submitComment = async () => {
    if (!commentText.trim()) return;
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }
    setBusy(true);
    try {
      await createComment(postId, commentText.trim());
      setCommentText("");
      setComments(await fetchComments(postId)); // 서버 기준으로 다시 읽는다
    } catch (e) {
      setError(e instanceof Error ? e.message : "댓글을 남기지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const removeComment = async (commentId: number) => {
    setBusy(true);
    try {
      await deleteComment(commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
    } catch (e) {
      setError(e instanceof Error ? e.message : "댓글을 지우지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const removePost = async () => {
    // 되돌릴 수 없는 동작이라 한 번 묻는다 (댓글도 함께 사라진다)
    if (!window.confirm("이 글을 삭제할까요? 댓글도 함께 사라집니다.")) return;
    setBusy(true);
    try {
      await deletePost(postId);
      navigate(`/work/${post!.workId}/posts`, { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : "글을 지우지 못했습니다.");
      setBusy(false);
    }
  };

  return (
    <Layout>
      {loading ? (
        <Box sx={{ mt: 3 }}>
          <Skeleton width="40%" height={44} />
          <Skeleton width="25%" />
          <Skeleton variant="rounded" height={200} sx={{ mt: 3 }} />
        </Box>
      ) : (
        post && (
          /* 폭 제한 없음 — Layout 의 Container(maxWidth:false)를 그대로 채운다.
             게시판 목록도 제한이 없어서, 목록↔상세를 오갈 때 폭이 튀지 않는다. */
          <Box sx={{ py: 2 }}>
            <Button
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate(`/work/${post.workId}/posts`)}
              sx={{ color: "text.secondary" }}
            >
              {post.workTitle} 게시판
            </Button>

            {/* 본문 + 사이드바 — 좁은 화면에서는 사이드바가 아래로 내려간다 */}
            <Stack direction={{ xs: "column", lg: "row" }} spacing={3} sx={{ mt: 1 }}>
              <Box sx={{ flex: 1, minWidth: 0 }}>

            {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

            <Card elevation={0} sx={{ mt: 2, p: 3, borderRadius: 3, border: "1px solid #e5eaf2", bgcolor: "#fff" }}>
              <Typography variant="h5" sx={{ fontWeight: 800, color: "#1b2a4a" }}>
                {post.title}
              </Typography>

              <Stack
                direction="row"
                spacing={2}
                sx={{ mt: 1, alignItems: "center", flexWrap: "wrap", color: "text.secondary" }}
              >
                <Typography variant="body2" sx={{ fontWeight: 700, color: "#0891b2" }}>
                  {post.authorNickname}
                </Typography>
                <Typography variant="body2">{formatPostDate(post.createdAt)}</Typography>
                <Typography variant="body2">조회 {post.viewCount}</Typography>

                {/* 작성자에게만 수정·삭제 (서버도 403으로 막지만, 눌러도 안 되는 버튼은 보여주지 않는다) */}
                {post.mine && (
                  <Stack direction="row" spacing={0.5} sx={{ ml: "auto" }}>
                    <Button size="small" startIcon={<EditIcon />} onClick={() => navigate(`/post/${post.id}/edit`)}>
                      수정
                    </Button>
                    <Button size="small" color="error" startIcon={<DeleteOutlineIcon />} onClick={removePost} disabled={busy}>
                      삭제
                    </Button>
                  </Stack>
                )}
              </Stack>

              <Divider sx={{ my: 2.5 }} />

              {/* 줄바꿈을 그대로 살린다 — 에디터가 없으므로 입력한 형태가 곧 표시 형태다 */}
              <Typography sx={{ color: "text.primary", lineHeight: 1.9, whiteSpace: "pre-wrap" }}>
                {post.content}
              </Typography>

              <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
                <Button
                  variant={post.likedByMe ? "contained" : "outlined"}
                  color="error"
                  startIcon={post.likedByMe ? <ThumbUpIcon /> : <ThumbUpOutlinedIcon />}
                  onClick={toggleLike}
                  sx={{ fontWeight: 700, borderRadius: 8, px: 3 }}
                >
                  추천 {post.likeCount}
                </Button>
              </Box>
            </Card>

            {/* ── 댓글 ── */}
            <Typography sx={{ fontWeight: 800, color: "#1b2a4a", mt: 4, mb: 1.5 }}>
              댓글 <Box component="span" sx={{ color: "#0891b2" }}>{comments.length}</Box>
            </Typography>

            <Card elevation={0} sx={{ borderRadius: 3, border: "1px solid #e5eaf2", bgcolor: "#fff" }}>
              {comments.length === 0 ? (
                <Box sx={{ py: 5, textAlign: "center" }}>
                  <Typography color="text.secondary" variant="body2">첫 댓글을 남겨보세요.</Typography>
                </Box>
              ) : (
                comments.map((c, i) => (
                  <Box key={c.id}>
                    {i > 0 && <Divider />}
                    <Box sx={{ p: 2 }}>
                      <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
                        <Typography variant="body2" sx={{ fontWeight: 700, color: "#0891b2" }}>
                          {c.authorNickname}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {formatPostDate(c.createdAt)}
                        </Typography>
                        {c.mine && (
                          <IconButton
                            size="small"
                            onClick={() => removeComment(c.id)}
                            disabled={busy}
                            aria-label="댓글 삭제"
                            sx={{ ml: "auto" }}
                          >
                            <DeleteOutlineIcon sx={{ fontSize: 16 }} />
                          </IconButton>
                        )}
                      </Stack>
                      <Typography sx={{ mt: 0.5, whiteSpace: "pre-wrap" }}>{c.content}</Typography>
                    </Box>
                  </Box>
                ))
              )}
            </Card>

            {/* 댓글 입력 — 비로그인에게도 보여주고, 누르면 로그인으로 안내한다 */}
            <Stack direction="row" spacing={1} sx={{ mt: 2, alignItems: "flex-start" }}>
              <TextField
                placeholder={isLoggedIn ? "댓글을 입력하세요" : "로그인하면 댓글을 남길 수 있어요"}
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                disabled={busy}
                size="small"
                multiline
                maxRows={4}
                fullWidth
                slotProps={{ htmlInput: { maxLength: 1000 } }}
              />
              <Button
                variant="contained"
                onClick={submitComment}
                disabled={busy || !commentText.trim()}
                sx={{ fontWeight: 700, whiteSpace: "nowrap", py: 1 }}
              >
                등록
              </Button>
            </Stack>

              </Box>

              {/* ★alignSelf 필수★ Stack(row)의 기본값은 alignItems:stretch 라
                  사이드바가 본문 높이만큼 늘어난다. sticky 도 그때는 동작하지 않는다. */}
              <Box
                sx={{
                  width: { xs: "100%", lg: 320 },
                  flexShrink: 0,
                  alignSelf: "flex-start",
                  position: { lg: "sticky" },
                  top: 16,
                }}
              >
                <PostSidebar workId={post.workId} currentPostId={post.id} />
              </Box>
            </Stack>
          </Box>
        )
      )}
    </Layout>
  );
}

export default PostDetailPage;
