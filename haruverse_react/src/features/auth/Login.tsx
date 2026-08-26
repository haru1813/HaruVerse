import { useState } from "react";
import { Box, Card, Typography, TextField, Button, Link, Stack, Alert } from "@mui/material";
import { useNavigate } from "react-router-dom";
import Layout from "../../layouts/Layout";
import { useAuth } from "../../contexts/AuthContext";

// 로그인 화면 — POST /api/auth/login 으로 인증 후 JWT 토큰을 저장.
function Login() {
  const navigate = useNavigate();
  const { login } = useAuth(); // 전역 인증 상태에 로그인 정보를 반영 → 헤더가 즉시 바뀜

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!email || !password) {
      setError("이메일과 비밀번호를 입력해주세요.");
      return;
    }

    try {
      setLoading(true);
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (res.ok) {
        // 백엔드 LoginResponse { token, email, nickname }
        const data = await res.json();
        // 토큰 + 회원정보를 저장하고 전역 상태를 갱신
        // (localStorage에만 넣으면 React가 모르기 때문에 헤더가 안 바뀐다)
        login(data.token, { email: data.email, nickname: data.nickname });
        navigate("/"); // 홈으로 이동
        return;
      }

      // 401 등 실패 → 백엔드 메시지 표시
      const data = await res.json().catch(() => null);
      setError(data?.message ?? "로그인에 실패했습니다.");
    } catch {
      setError("서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인해주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <Box sx={{ display: "flex", justifyContent: "center", py: { xs: 4, md: 8 } }}>
        <Card
          elevation={0}
          sx={{
            width: "100%",
            maxWidth: 420,
            p: { xs: 3, md: 4 },
            borderRadius: 3,
            border: "1px solid #e5eaf2",
            bgcolor: "#fff",
          }}
        >
          <Typography variant="h5" sx={{ fontWeight: 800, color: "#1b2a4a", textAlign: "center" }}>
            로그인
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", mt: 1, mb: 3 }}>
            Haru<span style={{ color: "#38bdf8", fontWeight: 700 }}>Verse</span>에 오신 걸 환영해요
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <form onSubmit={handleSubmit} noValidate>
            <Stack spacing={2}>
              <TextField
                label="이메일"
                type="email"
                fullWidth
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <TextField
                label="비밀번호"
                type="password"
                fullWidth
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <Button
                type="submit"
                variant="contained"
                color="secondary"
                size="large"
                fullWidth
                disabled={loading}
                sx={{ fontWeight: 700, py: 1.2, color: "#fff" }}
              >
                {loading ? "로그인 중…" : "로그인"}
              </Button>
            </Stack>
          </form>

          {/* 보조 링크 — 비밀번호 찾기 아래 회원가입 */}
          <Stack spacing={1.2} sx={{ mt: 3, alignItems: "center" }}>
            <Link href="#" underline="hover" sx={{ fontSize: 14, color: "text.secondary" }}>
              비밀번호 찾기
            </Link>
            <Link
              component="button"
              type="button"
              onClick={() => navigate("/signup")}
              underline="hover"
              sx={{ fontSize: 14, color: "#0891b2", fontWeight: 700 }}
            >
              회원가입
            </Link>
          </Stack>
        </Card>
      </Box>
    </Layout>
  );
}

export default Login;
