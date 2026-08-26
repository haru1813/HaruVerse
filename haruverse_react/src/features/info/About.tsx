import { Box, Chip, Stack, Typography } from "@mui/material";
import InfoPage, { Section } from "./InfoPage";

const STACK = [
  { group: "프론트엔드", items: ["React 19", "TypeScript", "Vite", "MUI", "React Router"] },
  { group: "백엔드", items: ["Spring Boot 3", "Spring Security", "JWT", "JPA / Hibernate", "MyBatis"] },
  { group: "데이터", items: ["H2 (개발)", "MariaDB (운영 예정)"] },
  { group: "테스트", items: ["JUnit 5", "Playwright E2E"] },
];

// 소개 — /about
function About() {
  return (
    <InfoPage title="소개" subtitle="HaruVerse가 무엇이고, 무엇으로 만들었는지">
      <Section title="HaruVerse란">
        애니메이션과 게임을 <b>한곳에서 검색하는 통합 도감</b>입니다.
        보통 애니는 애니 사이트에서, 게임은 게임 사이트에서 따로 찾아야 합니다.
        HaruVerse는 서로 다른 두 출처의 데이터를 같은 형태로 정리해 한 화면에서 다룹니다.
      </Section>

      <Section title="할 수 있는 일">
        <ul>
          <li>제목으로 작품 검색 · 장르와 분류로 필터링</li>
          <li>작품 상세 정보 확인 (줄거리 · 제작사 · 장르 · 평점)</li>
          <li>회원가입 후 마음에 드는 작품 <b>찜하기</b> → 마이페이지에서 모아보기</li>
        </ul>
      </Section>

      <Section title="데이터 출처">
        애니메이션은 <b>Jikan API</b>(MyAnimeList 비공식 API), 게임은 <b>RAWG</b>에서 가져옵니다.
        <br />
        두 서비스의 평점 체계가 달라(애니 0~10, 게임 0~5) 그대로 두면 게임이 저평가된 것처럼 보이므로,
        <b> 10점 만점으로 환산해 통일</b>했습니다.
      </Section>

      <Section title="기술 스택">
        <Stack spacing={1.5} sx={{ mt: 1 }}>
          {STACK.map(({ group, items }) => (
            <Box key={group}>
              <Typography variant="caption" sx={{ color: "#0891b2", fontWeight: 700 }}>
                {group}
              </Typography>
              <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.8, mt: 0.5 }}>
                {items.map((i) => (
                  <Chip
                    key={i}
                    label={i}
                    size="small"
                    sx={{ bgcolor: "rgba(56,189,248,0.12)", color: "#0891b2", fontWeight: 600 }}
                  />
                ))}
              </Box>
            </Box>
          ))}
        </Stack>
      </Section>

      <Section title="만든 사람">
        개인 포트폴리오로 만든 프로젝트입니다. 기획 · 설계 · 프론트엔드 · 백엔드 · 배포를 혼자 맡았습니다.
      </Section>
    </InfoPage>
  );
}

export default About;
