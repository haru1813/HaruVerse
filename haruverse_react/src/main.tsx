import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material'
import AuthProvider from './contexts/AuthProvider.tsx'
import FavoriteProvider from './contexts/FavoriteProvider.tsx'
import SubscriptionProvider from './contexts/SubscriptionProvider.tsx'
import App from './App.tsx'

// HaruVerse 테마 — 설계문서의 네이비·블루·시안 팔레트
const theme = createTheme({
  palette: {
    primary: { main: '#2563eb' },   // 블루
    secondary: { main: '#38bdf8' }, // 시안 (HaruVerse 포인트 — 로고 'Verse'와 동일)
    // ★페이지 배경 = 흰색★ 카드가 bgcolor:'transparent' 라 이 값이 그대로 카드 안쪽 색이 된다.
    // #f6f8fc(옅은 청회색)였을 때 카드 안쪽만 회색으로 떠 보였다.
    // 카드·패널은 전부 border:1px solid #e5eaf2 를 갖고 있어 흰 배경에서도 형태가 남는다.
    background: { default: '#ffffff' },
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: '"Pretendard","Malgun Gothic","맑은 고딕",system-ui,sans-serif',
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* ThemeProvider = 앱 전체에 테마 주입 / CssBaseline = MUI 기본 스타일 리셋 */}
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {/* BrowserRouter = URL 기반 라우팅을 앱 전체에 주입 */}
      <BrowserRouter>
        {/* AuthProvider = 로그인 상태를 앱 전체에 주입 (헤더·마이페이지가 함께 참조) */}
        <AuthProvider>
          {/* FavoriteProvider = 찜 목록을 앱 전체에 주입.
              AuthProvider '안쪽'이어야 한다 — 로그인 상태를 구독하기 때문 */}
          <FavoriteProvider>
            {/* SubscriptionProvider = 채널 구독 상태를 앱 전체에 주입.
                찜과 마찬가지로 AuthProvider '안쪽'이어야 한다 */}
            <SubscriptionProvider>
              <App />
            </SubscriptionProvider>
          </FavoriteProvider>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>,
)
