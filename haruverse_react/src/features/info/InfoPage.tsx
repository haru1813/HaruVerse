import type { ReactNode } from "react";
import { Box, Card, Typography } from "@mui/material";
import Layout from "../../layouts/Layout";

/**
 * 정보성 정적 페이지의 공통 껍데기 (소개·이용약관·개인정보처리방침·문의).
 *
 * <p>네 페이지가 제목 스타일과 카드 여백을 공유하므로 한 곳에 모아둔다.
 */
function InfoPage({
  title,
  subtitle,
  updatedAt,
  children,
}: {
  title: string;
  subtitle?: string;
  /** 약관·방침처럼 개정일 표기가 필요한 문서에만 */
  updatedAt?: string;
  children: ReactNode;
}) {
  return (
    <Layout>
      <Box sx={{ maxWidth: 880, mx: "auto", py: { xs: 3, md: 5 } }}>
        <Typography
          variant="h4"
          sx={{
            fontWeight: 800,
            color: "#1b2a4a",
            position: "relative",
            pl: 2,
            "&::before": {
              content: '""',
              position: "absolute",
              left: 0,
              top: 6,
              bottom: 6,
              width: 5,
              borderRadius: 2,
              bgcolor: "#38bdf8",
            },
          }}
        >
          {title}
        </Typography>

        {subtitle && (
          <Typography color="text.secondary" sx={{ mt: 1, pl: 2 }}>
            {subtitle}
          </Typography>
        )}

        <Card
          elevation={0}
          sx={{
            mt: 3,
            p: { xs: 3, md: 4 },
            borderRadius: 3,
            border: "1px solid #e5eaf2",
            bgcolor: "#fff",
          }}
        >
          {children}

          {updatedAt && (
            <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 4 }}>
              최종 개정일 · {updatedAt}
            </Typography>
          )}
        </Card>
      </Box>
    </Layout>
  );
}

/** 문서 안의 소제목 + 본문 한 덩어리 */
export function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Box sx={{ "&:not(:first-of-type)": { mt: 3.5 } }}>
      <Typography sx={{ fontWeight: 800, color: "#1b2a4a", mb: 1 }}>{title}</Typography>
      <Box
        sx={{
          color: "text.secondary",
          lineHeight: 1.9,
          fontSize: 15,
          "& ul": { pl: 2.5, m: 0 },
          "& li": { mb: 0.5 },
          "& b": { color: "#1b2a4a" },
        }}
      >
        {children}
      </Box>
    </Box>
  );
}

export default InfoPage;
