import InfoPage, { Section } from "./InfoPage";

// 이용약관 — /terms
function Terms() {
  return (
    <InfoPage
      title="이용약관"
      subtitle="HaruVerse 서비스 이용에 관한 안내"
      updatedAt="2026년 8월 25일"
    >
      <Section title="제1조 (성격)">
        HaruVerse는 <b>개인이 학습·포트폴리오 목적으로 운영하는 비영리 서비스</b>입니다.
        상업적 서비스가 아니며, 회사나 단체가 운영하지 않습니다.
      </Section>

      <Section title="제2조 (회원가입과 계정)">
        <ul>
          <li>이메일과 비밀번호로 가입하며, 찜하기 등 개인화 기능을 이용할 수 있습니다.</li>
          <li>계정 정보는 본인이 관리해야 하며, 타인과 공유해 발생한 문제에는 책임지지 않습니다.</li>
          <li>실제 개인정보 대신 임의의 값으로 가입하셔도 무방합니다.</li>
        </ul>
      </Section>

      <Section title="제3조 (콘텐츠와 저작권)">
        작품 정보 · 이미지 · 줄거리는 <b>Jikan API</b>와 <b>RAWG</b>에서 제공받으며,
        각 콘텐츠의 저작권은 원저작권자에게 있습니다. HaruVerse는 이를 검색·열람 목적으로 표시할 뿐
        소유하거나 판매하지 않습니다.
      </Section>

      <Section title="제4조 (정확성에 대한 면책)">
        외부 API가 제공하는 정보를 그대로 사용하므로 <b>내용의 정확성이나 최신성을 보장하지 않습니다.</b>
        외부 서비스 장애 시 일부 정보가 표시되지 않을 수 있습니다.
      </Section>

      <Section title="제5조 (서비스 변경과 중단)">
        개인이 운영하는 특성상 <b>사전 고지 없이 기능이 바뀌거나 중단될 수 있으며,
        저장된 데이터가 삭제될 수 있습니다.</b> 중요한 정보를 이곳에만 보관하지 마시기 바랍니다.
      </Section>

      <Section title="제6조 (금지 행위)">
        <ul>
          <li>자동화 도구로 과도한 요청을 보내 서비스 운영을 방해하는 행위</li>
          <li>타인의 계정에 무단으로 접근하려는 행위</li>
          <li>수집된 데이터를 상업적 목적으로 재배포하는 행위</li>
        </ul>
      </Section>
    </InfoPage>
  );
}

export default Terms;
