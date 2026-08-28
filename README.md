# HaruVerse

애니메이션·게임 **통합 도감 + 커뮤니티**.
외부 공개 API로 수집한 작품을 검색·필터하고, 작품별 게시판에서 이야기할 수 있습니다.

> PHP 백엔드 경력자가 **Spring Boot 전환**을 증명하려고 만든 개인 프로젝트입니다.
> 상거래·결제가 없는 정보형 서비스라, 도메인 모델링과 외부 연동에 집중했습니다.

```
Spring Boot 3.4 / Java 21 · React 19 / TypeScript · MariaDB 11 · Docker Compose
백엔드 테스트 149 (JUnit) · E2E 156 (Playwright)
```

---

## 무엇을 만들었나

| 도메인 | 내용 |
|---|---|
| **work** | 작품 187편 (애니 86 · 게임 101). 장르 N:M, 제작사 N:1, 게임 플랫폼 |
| **character** | 캐릭터 274명 — 작품과 N:M, 연결에 배역(주연/조연) 보관 |
| **voiceactor** | 성우 147명 — 캐릭터 상세에서 성우로, 성우에서 다른 배역으로 왕복 |
| **studio** | 제작사 92곳 |
| **member / auth** | 회원가입·로그인 (JWT) |
| **favorite** | 찜 — *이 작품이 좋다* (도감 관점) |
| **community** | 작품별 게시판·댓글·추천, 채널 구독 — *이 게시판을 읽겠다* (커뮤니티 관점) |

데이터는 **공개 API**에서 수집합니다. 애니는 [Jikan](https://jikan.moe)(MyAnimeList 비공식), 게임은 [RAWG](https://rawg.io). 크롤링이 아니라 공개 API라 이용약관 안에서 동작합니다.

---

## 설계에서 고민한 것들

포트폴리오의 핵심은 기능 개수가 아니라 **왜 그렇게 했는가**라고 생각해서, 판단이 갈렸던 지점을 남깁니다. 코드 주석에도 같은 내용이 달려 있습니다.

### 찜과 구독을 왜 따로 두었나

표가 똑같습니다. 둘 다 `(member_id, work_id)` 한 쌍이라 합치고 싶어집니다.
하지만 **뜻이 다릅니다.** 찜은 "이 작품이 좋다"(도감), 구독은 "이 게시판을 읽겠다"(커뮤니티)입니다.
작품엔 관심 없어도 정보글만 챙겨 보는 사람이 있고, 인생작이라 찜은 했지만 게시판은 안 보는 사람도 있습니다.
하나로 합치면 **찜을 풀 때 구독이 함께 끊깁니다.**

표가 같다는 이유로 합치는 건 스키마를 보고 도메인을 정하는 순서 뒤집기입니다.

### 상태 변경을 토글이 아니라 PUT / DELETE 로

`POST /toggle` 은 같은 요청을 두 번 보내면 상태가 원래대로 돌아갑니다(멱등하지 않음).
네트워크 재시도나 더블클릭이면 사용자 의도와 **반대 결과**가 나옵니다.

```
PUT    /api/works/{id}/favorite      찜인 상태로 만들어라      → 201(신규) / 200(이미)
DELETE /api/works/{id}/favorite      찜이 아닌 상태로 만들어라  → 204 (원래 없어도)
```

### DataIntegrityViolationException 을 잡지 않는 이유

exists 검사와 INSERT 사이의 틈으로 동시 요청이 들어오면 유니크 제약에 걸립니다.
잡아서 "이미 있으니 성공"으로 처리하고 싶어지지만, **JPA는 INSERT가 터진 시점에 트랜잭션을 rollback-only 로 마킹합니다.**
예외를 삼켜도 커밋 때 `UnexpectedRollbackException` 으로 다시 터집니다.
→ 밖으로 내보내고 핸들러가 409로 응답합니다. 프론트는 낙관적 UI라 사용자 눈에는 문제가 없습니다.

### 수집 API 를 관리자만 부를 수 있게

`/api/collect/**` 는 Jikan·RAWG 를 **대신 호출**합니다. 로그인만 하면 누구나 부를 수 있게 두면,
가입한 아무나 외부 API 쿼터를 소진시키고 DB 를 오염시킬 수 있습니다.
→ JWT 에 `role` 클레임을 싣고 `hasRole("ADMIN")` 으로 막았습니다.

두 가지를 조심해야 했습니다.

**① 이미 행이 있는 표에 NOT NULL 컬럼은 그냥 안 붙습니다.**
`ddl-auto=update` 는 기존 행을 뭘로 채울지 몰라서 **조용히 건너뜁니다**(에러도 안 납니다).
DB 기본값을 함께 줘야 추가에 성공합니다.

```java
@Column(nullable = false, length = 20,
        columnDefinition = "varchar(20) not null default 'USER'")
private MemberRole role = MemberRole.USER;
```

**② 403 이 401 로 덮입니다.**
Spring Security 는 403 을 낼 때 `response.sendError(403)` 을 부르고, 서블릿은 `/error` 로 **다시 디스패치**합니다.
그 재요청도 필터 체인을 타는데 그때는 SecurityContext 가 비어 있어 `authenticated()` 에 걸립니다.
→ "권한 없음"이 "로그인하라"로 바뀝니다. **MockMvc 는 ERROR 디스패치를 타지 않아 테스트에서는 403 이 보입니다.**

```java
.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
```

| 요청자 | 결과 |
|---|---|
| 비로그인 | 401 |
| 일반 회원(USER) | **403** |
| 관리자(ADMIN) | 200 |

### 평점 스케일 통일

애니(Jikan)는 0~10, 게임(RAWG)은 0~5입니다. 같은 카드 목록에 섞이면 **게임이 전부 저평가된 것처럼 보입니다.**
metacritic(0~100)이 있으면 그쪽을 우선해 10점 만점으로 환산합니다.

### 목록 조회에 fetch join 을 쓰지 않은 이유

컬렉션을 조인하면 행이 뻥튀기되어 **페이징이 깨집니다**(Hibernate가 메모리에서 페이징하며 경고를 띄웁니다).
대신 `@BatchSize(100)` 으로 `where work_id in (?, ?, …)` 형태로 묶어 읽습니다.

### 찜 여부를 목록 응답에 넣지 않은 이유

카드 24장에 각각 찜 여부가 필요한데,
① 카드마다 조회 → 요청 24번, 논외
② `WorkResponse` 에 `favorited` 추가 → 작품 조회 서비스가 "누가 보는지"를 알아야 해서 비로그인/로그인 분기가 도메인으로 파고듭니다

→ `GET /api/favorites/ids` 로 id 배열을 한 번 받아 프론트가 `Set` 으로 대조합니다.
찜이 수만 건이 되면 못 쓰는 방식이라, 그때는 ②로 가되 조회 전용 쿼리를 짭니다.

### "그룹별 최신 1건" 을 JPQL 로 못 짜서

커뮤니티 첫 화면은 채널 카드마다 **최근 글**을 보여줍니다.
그런데 "그룹별 최신 한 건"은 JPQL 한 방으로 안 됩니다(서브쿼리에 limit 이 없습니다).
네이티브 쿼리면 되지만 DB에 묶입니다.

→ ① `group by` 로 작품별 글 수·최신 글 id 를 집계하고 ② 그 id 들만 한 번 더 읽습니다.
쿼리는 두 번이지만 **카드 수와 무관하게 항상 두 번**입니다(N+1 아님).
정렬은 `max(created_at)` 이 아니라 `max(id)` — 같은 시각 행이 있으면 중복이 생깁니다.

---

## 실행

### Docker (권장)

```bash
docker compose up -d --build
```

| 서비스 | 주소 |
|---|---|
| 프론트 | http://localhost:303 |
| 백엔드 | http://localhost:304/api/works |
| MariaDB | localhost:305 |
| Elasticsearch | http://localhost:306 |

게임 수집 API를 쓰려면 [RAWG 키](https://rawg.io/apidocs)가 필요합니다. **키는 소스에 두지 않습니다.**

```bash
export RAWG_API_KEY=...       # 없으면 게임 수집만 503, 나머지는 정상
export JWT_SECRET=...         # 운영에서는 반드시 지정 (HS256, 최소 32바이트)
docker compose up -d
```

### 로컬 개발

```bash
docker compose up -d mariadb              # DB만 컨테이너로

cd haruverse_springboot && ./gradlew bootRun     # :8080
cd haruverse_react && npm install && npm run dev # :5173
```

`docker-compose.yml` 의 백엔드와 로컬 백엔드가 **같은 DB를 봅니다.** 로컬에서 쓴 글이 컨테이너에도 그대로 보입니다.

### 테스트

```bash
cd haruverse_springboot && ./gradlew test        # 149
cd haruverse_react && npx playwright test        # 156
```

---

## API

인증이 필요 없는 것(공개 조회)과 필요한 것이 경로로 갈립니다.
`GET /api/works/**`·`/api/characters/**`·`/api/community/**` 는 공개, 나머지는 `authenticated()` 입니다.

| | 경로 |
|---|---|
| 작품 | `GET /api/works` (검색·종류·장르·제작사 필터) · `GET /api/works/{id}` |
| 캐릭터 | `GET /api/characters` · `GET /api/characters/{id}` · `GET /api/works/{id}/characters` |
| 성우 | `GET /api/voice-actors` · `GET /api/voice-actors/{id}` |
| 제작사 | `GET /api/studios` |
| 인증 | `POST /api/auth/signup` · `POST /api/auth/login` · `GET /api/members/me` |
| 찜 | `PUT`·`DELETE /api/works/{id}/favorite` · `GET /api/favorites` · `GET /api/favorites/ids` |
| 커뮤니티 | `GET /api/community/channels` · `GET`·`POST /api/works/{id}/posts` · `GET`·`PUT`·`DELETE /api/posts/{id}` |
| 댓글·추천 | `POST /api/posts/{id}/comments` · `PUT`·`DELETE /api/posts/{id}/like` |
| 구독 | `PUT`·`DELETE /api/works/{id}/subscription` · `GET /api/subscriptions` |

> 내 구독 목록을 `/api/community/` 아래 두지 않은 이유가 있습니다.
> `GET /api/community/**` 는 permitAll 이라(커뮤니티는 비로그인도 읽어야 하므로 맞는 설정),
> 거기 두면 **비로그인 요청이 인증 없이 통과해** principal 이 null 인 채 서비스까지 도달합니다.

---

## 구조

```
haruverse_springboot/    Spring Boot — 도메인별 패키지 (Package by Feature)
  work/ character/ voiceactor/ studio/ genre/
  member/ auth/ favorite/ community/
  external/  jikan · rawg · starrail   외부 API 연동
  global/    jwt · exception · response  공통

haruverse_react/         React + Vite + MUI
  features/    화면 단위 (work · character · community · favorite · info · auth)
  components/  공유 UI (WorkCard · WorkGrid · FavoriteButton · SubscribeButton)
  contexts/    Auth · Favorite · Subscription  (Context/Provider 분리 — Fast Refresh)
  e2e/         Playwright

docs/                    설계 문서 (기획 · 패키지 구조 · API 명세 · ERD · 화면 설계)
```

모듈러 모놀리식으로 시작했습니다. 도메인 경계를 넘는 연관은 ID 참조로 두어, 나중에 쪼갤 여지를 남겼습니다.

---

## 아직 안 된 것

숨기는 것보다 적어두는 편이 낫다고 생각해서 남깁니다.

- **한글 제목 나머지 21편** — TMDB 매칭이 애니 86편 중 65편(76%)에서 성공했습니다.
  못 채운 건 대부분 시즌 표기가 특이하거나 TMDB 에 없는 작품입니다.
  ★확신이 없으면 채우지 않습니다★ — 틀린 제목은 비어 있는 것보다 나쁩니다.
- **캐릭터 데이터** — 애니 86편 중 캐릭터가 채워진 건 2편뿐입니다. MyAnimeList 가 장기 장애(504) 중이라 수집이 막혀 있습니다. (3회 연속 실패하면 중단하도록 되어 있습니다)
- **배포** — 맥미니 셀프호스팅으로 올릴 예정입니다.
- 게시글 검색, 이미지 업로드, 대댓글

---

## 데이터 출처

- 애니메이션 — [Jikan API](https://jikan.moe) (MyAnimeList 비공식 API)
- 게임 — [RAWG Video Games Database](https://rawg.io)
- 한국어 제목 — [TMDB](https://www.themoviedb.org)

비상업적 개인 프로젝트입니다. 모든 작품 정보·이미지의 저작권은 원 권리자에게 있습니다.
