import { Routes, Route } from "react-router-dom";
import Home from "./features/home/Home";
import Login from "./features/auth/Login";
import Signup from "./features/auth/Signup";
import MyPage from "./features/member/MyPage";
import WorkDetailPage from "./features/work/WorkDetailPage";
import About from "./features/info/About";
import Terms from "./features/info/Terms";
import Privacy from "./features/info/Privacy";
import Contact from "./features/info/Contact";
import CharacterList from "./features/character/CharacterList";
import CharacterDetailPage from "./features/character/CharacterDetailPage";
import StudioList from "./features/studio/StudioList";
import VoiceActorList from "./features/voiceactor/VoiceActorList";
import VoiceActorDetailPage from "./features/voiceactor/VoiceActorDetailPage";
import CommunityPage from "./features/community/CommunityPage";
import PostListPage from "./features/community/PostListPage";
import PostFormPage from "./features/community/PostFormPage";
import PostDetailPage from "./features/community/PostDetailPage";
import ScrollToTop from "./components/ScrollToTop";

function App() {
  return (
    <>
      {/* 라우트가 바뀔 때 화면을 맨 위로 — SPA는 스크롤을 자동으로 되돌리지 않는다 */}
      <ScrollToTop />

      {/* 라우트 정의 — path에 맞는 페이지가 렌더됨 */}
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        {/* 마이페이지 — 비로그인 상태로 들어오면 MyPage 안에서 /login으로 돌려보냄 */}
        <Route path="/mypage" element={<MyPage />} />
        {/* 작품 상세 — 목록 카드 클릭 시 이동 */}
        <Route path="/work/:id" element={<WorkDetailPage />} />

        {/* 캐릭터 도감 — 헤더의 '캐릭터' 탭에서 진입 */}
        <Route path="/characters" element={<CharacterList />} />
        <Route path="/character/:id" element={<CharacterDetailPage />} />

        {/* 제작사 목록 — 고르면 홈의 studio 필터로 보낸다 */}
        <Route path="/studios" element={<StudioList />} />

        {/* 성우 도감 — 캐릭터에서 성우로, 성우에서 다시 캐릭터로 오갈 수 있다 */}
        <Route path="/voice-actors" element={<VoiceActorList />} />
        <Route path="/voice-actor/:id" element={<VoiceActorDetailPage />} />

        {/* 커뮤니티 — 게시판은 작품에 딸려 있다 (작품 = 채널)
            /community 는 모든 게시판의 최근 글을 모아 보여주는 입구 */}
        <Route path="/community" element={<CommunityPage />} />
        <Route path="/work/:workId/posts" element={<PostListPage />} />
        <Route path="/work/:workId/posts/new" element={<PostFormPage mode="create" />} />
        <Route path="/post/:id" element={<PostDetailPage />} />
        <Route path="/post/:id/edit" element={<PostFormPage mode="edit" />} />

        {/* 정보성 정적 페이지 — 푸터에서 연결된다 */}
        <Route path="/about" element={<About />} />
        <Route path="/terms" element={<Terms />} />
        <Route path="/privacy" element={<Privacy />} />
        <Route path="/contact" element={<Contact />} />
        {/* TODO(하루): /search 검색 라우트 추가 */}
      </Routes>
    </>
  );
}

export default App;
