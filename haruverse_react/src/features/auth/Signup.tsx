import { useState } from "react";
import { Box, Card, Typography, TextField, Button, Link, Stack, Alert } from "@mui/material";
import { useNavigate } from "react-router-dom";
import Layout from "../../layouts/Layout";

// 회원가입 화면 — 폼 입력을 받아 백엔드 POST /api/auth/signup 으로 가입 요청.
function Signup() {
  const navigate = useNavigate();

  // 폼 입력값 상태 — 입력할 때마다 여기에 저장됨(제어 컴포넌트)
  const [email, setEmail] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");

  // 화면 상태 — 에러 메시지 / 요청 중(중복 클릭 방지)
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // 비밀번호 확인 불일치 여부 (확인란에 뭔가 입력했을 때만 검사)
  const passwordMismatch = passwordConfirm.length > 0 && password !== passwordConfirm;

  // 가입하기 제출 핸들러
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); // 폼 기본 동작(새로고침) 막기
    setError("");

    // 1) 프론트 검증 — 필수값 / 비밀번호 일치
    if (!email || !nickname || !password || !passwordConfirm) {
      setError("모든 항목을 입력해주세요.");
      return;
    }
    if (password !== passwordConfirm) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }

    // 2) 백엔드로 가입 요청
    try {
      setLoading(true);
      const res = await fetch("/api/auth/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        // 백엔드 SignupRequest(email, password, nickname) 형태에 맞춰 전송
        body: JSON.stringify({ email, password, nickname }),
      });

      if (res.ok) {
        // 성공 → 로그인 화면으로 이동
        navigate("/login");
        return;
      }

      // 실패 → 백엔드가 준 에러 메시지 표시 (예: 이메일 중복 409)
      const data = await res.json().catch(() => null);
      setError(data?.message ?? "회원가입에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } catch {
      // 네트워크 오류 등 (백엔드 미실행 포함)
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
          {/* 제목 */}
          <Typography variant="h5" sx={{ fontWeight: 800, color: "#1b2a4a", textAlign: "center" }}>
            회원가입
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", mt: 1, mb: 3 }}>
            Haru<span style={{ color: "#38bdf8", fontWeight: 700 }}>Verse</span>의 회원이 되어보세요
          </Typography>

          {/* 에러 알림 — error 값이 있을 때만 표시 */}
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {/* 가입 폼 — form 으로 감싸 Enter 제출·버튼 제출 모두 handleSubmit 로 */}
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
                label="닉네임"
                fullWidth
                autoComplete="nickname"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
              />
              <TextField
                label="비밀번호"
                type="password"
                fullWidth
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <TextField
                label="비밀번호 확인"
                type="password"
                fullWidth
                autoComplete="new-password"
                value={passwordConfirm}
                onChange={(e) => setPasswordConfirm(e.target.value)}
                error={passwordMismatch}
                helperText={passwordMismatch ? "비밀번호가 일치하지 않습니다." : ""}
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
                {loading ? "가입 중…" : "가입하기"}
              </Button>
            </Stack>
          </form>

          {/* 로그인으로 이동 */}
          <Stack direction="row" spacing={0.5} sx={{ mt: 3, justifyContent: "center" }}>
            <Typography variant="body2" color="text.secondary">
              이미 계정이 있으신가요?
            </Typography>
            <Link
              component="button"
              type="button"
              onClick={() => navigate("/login")}
              underline="hover"
              sx={{ fontSize: 14, color: "#0891b2", fontWeight: 700 }}
            >
              로그인
            </Link>
          </Stack>
        </Card>
      </Box>
    </Layout>
  );
}

export default Signup;
