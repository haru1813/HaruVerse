import { Box, Button, Stack } from "@mui/material";
import EmailIcon from "@mui/icons-material/Email";
import GitHubIcon from "@mui/icons-material/GitHub";
import InfoPage, { Section } from "./InfoPage";
import { SITE } from "../../lib/site";

const { email: EMAIL, github: GITHUB } = SITE;

// 문의 — /contact
function Contact() {
  return (
    <InfoPage title="문의" subtitle="버그 제보 · 기능 제안 · 그 밖의 이야기">
      <Section title="연락처">
        아래로 연락 주시면 확인하는 대로 답변드립니다.
        <Stack direction="row" spacing={1.5} sx={{ mt: 2, flexWrap: "wrap", gap: 1.5 }}>
          <Button
            variant="contained"
            startIcon={<EmailIcon />}
            href={`mailto:${EMAIL}`}
            sx={{ fontWeight: 700 }}
          >
            이메일 보내기
          </Button>
          <Button
            variant="outlined"
            startIcon={<GitHubIcon />}
            href={GITHUB}
            target="_blank"
            rel="noopener noreferrer"
            sx={{ fontWeight: 700 }}
          >
            GitHub
          </Button>
        </Stack>
        <Box sx={{ mt: 2, fontSize: 14 }}>{EMAIL}</Box>
      </Section>

      <Section title="버그를 제보하실 때">
        아래 내용을 함께 알려주시면 원인을 훨씬 빨리 찾을 수 있습니다.
        <ul>
          <li>어떤 화면에서, 무엇을 눌렀을 때 생겼는지</li>
          <li>기대한 동작과 실제 동작</li>
          <li>사용 중인 브라우저 (Chrome · Safari 등)</li>
        </ul>
      </Section>

      <Section title="작품 정보가 잘못되어 있다면">
        작품 데이터는 <b>Jikan API</b>와 <b>RAWG</b>에서 그대로 받아옵니다.
        내용 자체의 오류는 원본 서비스에서 수정되어야 이곳에도 반영됩니다.
        다만 <b>표시가 깨지거나 비어 있는 문제</b>는 HaruVerse 쪽 문제일 수 있으니 알려주세요.
      </Section>
    </InfoPage>
  );
}

export default Contact;
