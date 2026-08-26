import { useEffect, useState } from "react";
import { Box, Typography, TextField, Button, Stack, Alert, Card } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import { useNavigate, useParams } from "react-router-dom";
import Layout from "../../layouts/Layout";
import { useAuth } from "../../contexts/AuthContext";
import { createPost, fetchPost, updatePost } from "./api";

/**
 * 글쓰기 · 수정 — /work/:workId/posts/new, /post/:id/edit
 *
 * <p>두 화면이 폼도 검증도 같아서 하나로 합쳤다.
 * postId 가 있으면 수정, 없으면 새 글이다.
 */
function PostFormPage({ mode }: { mode: "create" | "edit" }) {
  const { workId: workIdParam, id: postIdParam } = useParams<{ workId: string; id: string }>();
  const navigate = useNavigate();
  const { isLoggedIn } = useAuth();

  const workId = Number(workIdParam);
  const postId = Number(postIdParam);
  const isEdit = mode === "edit";

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  // 비로그인 상태로 주소를 직접 친 경우 → 로그인 화면으로.
  // replace: true 로 두어 뒤로가기 시 다시 이 화면으로 튕기지 않게 한다.
  useEffect(() => {
    if (!isLoggedIn) navigate("/login", { replace: true });
  }, [isLoggedIn, navigate]);

  // 수정 모드면 기존 내용을 불러온다
  useEffect(() => {
    if (!isEdit || !Number.isFinite(postId)) return;
    let alive = true;

    fetchPost(postId)
      .then((p) => {
        if (!alive) return;
        setTitle(p.title);
        setContent(p.content);
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
  }, [isEdit, postId]);

  const submit = async () => {
    if (!title.trim() || !content.trim()) {
      setError("제목과 내용을 모두 입력해주세요.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      if (isEdit) {
        await updatePost(postId, title.trim(), content.trim());
        navigate(`/post/${postId}`, { replace: true });
      } else {
        await createPost(workId, title.trim(), content.trim());
        // 새 글의 id 를 응답 바디로 받지 않으므로(201 + Location) 게시판으로 돌아간다.
        // 방금 쓴 글이 최신순 맨 위에 있다.
        navigate(`/work/${workId}/posts`, { replace: true });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate(-1)}
        sx={{ mt: 2, color: "text.secondary" }}
      >
        취소
      </Button>

      <Box sx={{ maxWidth: 860, mx: "auto", py: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 800, color: "#1b2a4a", mb: 2 }}>
          {isEdit ? "글 수정" : "글쓰기"}
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: "1px solid #e5eaf2", bgcolor: "#fff" }}>
          <Stack spacing={2}>
            <TextField
              label="제목"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={loading || saving}
              slotProps={{ htmlInput: { maxLength: 200 } }}
              fullWidth
            />
            <TextField
              label="내용"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              disabled={loading || saving}
              slotProps={{ htmlInput: { maxLength: 10000 } }}
              multiline
              minRows={12}
              fullWidth
            />

            <Stack direction="row" spacing={1.5} sx={{ justifyContent: "flex-end" }}>
              <Button onClick={() => navigate(-1)} disabled={saving} sx={{ fontWeight: 700 }}>
                취소
              </Button>
              <Button
                variant="contained"
                onClick={submit}
                disabled={loading || saving}
                sx={{ fontWeight: 700, minWidth: 100 }}
              >
                {saving ? "저장 중…" : isEdit ? "수정" : "등록"}
              </Button>
            </Stack>
          </Stack>
        </Card>
      </Box>
    </Layout>
  );
}

export default PostFormPage;
