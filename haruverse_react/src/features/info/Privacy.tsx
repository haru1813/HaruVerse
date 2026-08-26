import { Box, Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import InfoPage, { Section } from "./InfoPage";

// 수집 항목 — 실제 구현(SignupRequest)과 일치해야 한다.
// 항목이 늘거나 줄면 여기도 반드시 같이 고칠 것.
const COLLECTED = [
  { item: "이메일", why: "계정 식별 · 로그인", how: "그대로 저장" },
  { item: "비밀번호", why: "본인 확인", how: "BCrypt로 단방향 암호화 (원문을 저장하지 않음)" },
  { item: "닉네임", why: "화면 표시", how: "그대로 저장" },
  { item: "찜한 작품", why: "마이페이지 목록 제공", how: "회원 번호와 작품 번호의 연결로 저장" },
];

// 개인정보처리방침 — /privacy
function Privacy() {
  return (
    <InfoPage
      title="개인정보처리방침"
      subtitle="어떤 정보를 왜 받고, 어떻게 다루는지"
      updatedAt="2026년 8월 25일"
    >
      <Section title="1. 수집하는 항목">
        <Box sx={{ overflowX: "auto", mt: 1 }}>
          <Table size="small" sx={{ minWidth: 520 }}>
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 800, color: "#1b2a4a" }}>항목</TableCell>
                <TableCell sx={{ fontWeight: 800, color: "#1b2a4a" }}>이용 목적</TableCell>
                <TableCell sx={{ fontWeight: 800, color: "#1b2a4a" }}>저장 방식</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {COLLECTED.map((r) => (
                <TableRow key={r.item}>
                  <TableCell sx={{ fontWeight: 700, whiteSpace: "nowrap" }}>{r.item}</TableCell>
                  <TableCell>{r.why}</TableCell>
                  <TableCell>{r.how}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
        이름 · 전화번호 · 생년월일 · 결제 정보는 <b>받지 않습니다.</b>
      </Section>

      <Section title="2. 로그인 상태를 유지하는 방법">
        로그인하면 서버가 <b>JWT 토큰</b>을 발급하고, 브라우저의 <b>localStorage</b>에 보관합니다.
        토큰 유효기간은 <b>1시간</b>이며, 만료되면 다시 로그인해야 합니다.
        로그아웃하면 저장된 토큰이 즉시 삭제됩니다.
        <br />
        광고나 분석을 위한 추적 쿠키는 사용하지 않습니다.
      </Section>

      <Section title="3. 제3자 제공">
        수집한 개인정보를 <b>어디에도 제공하거나 판매하지 않습니다.</b>
        <br />
        다만 작품 정보를 불러오기 위해 외부 API(Jikan · RAWG)를 호출하며,
        이때 <b>회원 정보는 전송되지 않습니다.</b> 작품 검색어와 식별자만 전달됩니다.
      </Section>

      <Section title="4. 보관 기간과 파기">
        회원 정보는 탈퇴 시까지 보관합니다. 다만 이 서비스는 개인 포트폴리오 프로젝트이므로
        <b> 개발 과정에서 데이터베이스 전체가 초기화될 수 있습니다.</b>
      </Section>

      <Section title="5. 이용자의 권리">
        본인의 정보 열람 · 수정 · 삭제를 원하시면 문의 페이지의 연락처로 요청해 주세요.
        가입 시 실제 개인정보 대신 임의의 이메일을 사용하셔도 모든 기능을 이용할 수 있습니다.
      </Section>

      <Section title="6. 보안을 위해 하고 있는 것">
        <ul>
          <li>비밀번호는 BCrypt로 단방향 암호화해 저장합니다 (운영자도 원문을 알 수 없습니다)</li>
          <li>인증이 필요한 요청은 서버에서 토큰을 검증합니다</li>
          <li>외부 API 키는 소스 코드가 아닌 환경변수로 주입합니다</li>
        </ul>
      </Section>
    </InfoPage>
  );
}

export default Privacy;
