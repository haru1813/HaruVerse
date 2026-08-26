import { useEffect, useState } from "react";
import { Box, Card, Typography, Stack, Avatar, Divider, Button, Alert, Skeleton, Chip } from "@mui/material";
import { useNavigate } from "react-router-dom";
import Layout from "../../layouts/Layout";
import { useAuth } from "../../contexts/AuthContext";
import { apiFetch, UnauthorizedError } from "../../lib/api";
import FavoriteList from "../favorite/FavoriteList";

// 백엔드 MemberResponse 와 대응
type MemberResponse = {
  id: number;
  email: string;
  nickname: string;
};

// 마이페이지 — 보호된 API(GET /api/members/me)를 호출해 내 정보를 보여준다.
// localStorage의 값을 그냥 띄우지 않고 서버에 물어보는 이유:
//   ① 토큰이 아직 유효한지 실제로 검증된다 (만료면 401)
//   ② 다른 기기에서 정보가 바뀌었어도 최신값을 받는다
function MyPage() {
  const navigate = useNavigate();
  const { isLoggedIn, logout } = useAuth();

  const [member, setMember] = useState<MemberResponse | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 비로그인 상태로 URL을 직접 친 경우 → 로그인 화면으로
    // replace: true → 뒤로가기 했을 때 다시 /mypage로 튕기지 않게
    if (!isLoggedIn) {
      navigate("/login", { replace: true });
      return;
    }

    let alive = true; // 응답이 늦게 왔는데 화면이 이미 사라진 경우 setState 방지

    (async () => {
      try {
        const data = await apiFetch<MemberResponse>("/api/members/me");
        if (alive) setMember(data);
      } catch (e) {
        if (!alive) return;
        if (e instanceof UnauthorizedError) {
          // 토큰 만료·위조 → 전역 상태도 비우고 로그인 화면으로
          logout();
          navigate("/login", { replace: true });
          return;
        }
        setError(e instanceof Error ? e.message : "내 정보를 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [isLoggedIn, logout, navigate]);

  return (
    <Layout>
      <Box sx={{ display: "flex", justifyContent: "center", py: { xs: 3, md: 6 } }}>
        <Card
          elevation={0}
          sx={{
            width: "100%",
            maxWidth: 560,
            p: { xs: 3, md: 4 },
            borderRadius: 3,
            border: "1px solid #e5eaf2",
            bgcolor: "#fff",
          }}
        >
          <Typography variant="h5" sx={{ fontWeight: 800, color: "#1b2a4a" }}>
            마이페이지
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 3 }}>
            내 계정 정보를 확인할 수 있어요
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {loading ? (
            /* 로딩 중 — 레이아웃이 튀지 않게 실제 내용과 비슷한 크기의 스켈레톤 */
            <Stack spacing={2}>
              <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
                <Skeleton variant="circular" width={64} height={64} />
                <Box sx={{ flex: 1 }}>
                  <Skeleton width="40%" height={28} />
                  <Skeleton width="60%" height={20} />
                </Box>
              </Stack>
              <Skeleton variant="rounded" height={90} />
            </Stack>
          ) : (
            member && (
              <>
                {/* 프로필 요약 */}
                <Stack direction="row" spacing={2} sx={{ alignItems: "center" }}>
                  <Avatar sx={{ width: 64, height: 64, bgcolor: "#38bdf8", color: "#0f1a2e", fontWeight: 800, fontSize: 28 }}>
                    {member.nickname.charAt(0)}
                  </Avatar>
                  <Box>
                    <Typography sx={{ fontWeight: 800, fontSize: 20, color: "#1b2a4a" }}>{member.nickname}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {member.email}
                    </Typography>
                  </Box>
                </Stack>

                <Divider sx={{ my: 3 }} />

                {/* 상세 정보 — 라벨/값 한 줄씩 */}
                <Stack spacing={1.8}>
                  <InfoRow label="회원 번호" value={`#${member.id}`} />
                  <InfoRow label="이메일" value={member.email} />
                  <InfoRow label="닉네임" value={member.nickname} />
                  <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center" }}>
                    <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>
                      인증 상태
                    </Typography>
                    <Chip label="JWT 인증됨" size="small" color="secondary" sx={{ color: "#fff", fontWeight: 700 }} />
                  </Stack>
                </Stack>

                <Divider sx={{ my: 3 }} />

                <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
                  <Button variant="outlined" fullWidth onClick={() => navigate("/")} sx={{ fontWeight: 700 }}>
                    홈으로
                  </Button>
                  <Button
                    variant="contained"
                    color="error"
                    fullWidth
                    onClick={() => {
                      logout();
                      navigate("/");
                    }}
                    sx={{ fontWeight: 700 }}
                  >
                    로그아웃
                  </Button>
                </Stack>

                {/* TODO(하루): 닉네임 변경 · 비밀번호 변경 */}
              </>
            )
          )}
        </Card>
      </Box>

      {/* 내가 찜한 작품 — 계정 카드(560px)와 달리 화면 전체 폭을 쓴다 */}
      {isLoggedIn && <FavoriteList />}
    </Layout>
  );
}

// 라벨 + 값 한 줄 (반복되니 작은 컴포넌트로 분리)
function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" spacing={2} sx={{ justifyContent: "space-between", alignItems: "center" }}>
      <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600, whiteSpace: "nowrap" }}>
        {label}
      </Typography>
      <Typography variant="body2" sx={{ color: "#1b2a4a", fontWeight: 600, textAlign: "right", wordBreak: "break-all" }}>
        {value}
      </Typography>
    </Stack>
  );
}

export default MyPage;
