# ─────────────────────────────────────────────────────────────
# HaruVerse 프론트 (React 19 + Vite + TypeScript)
#
# 멀티스테이지 빌드:
#   ① build  — Node 로 정적 파일을 만든다
#   ② run    — Nginx 로 그 파일만 서빙한다
# 최종 이미지에 Node 도 node_modules 도 남지 않는다 (수백 MB → 수십 MB).
# ─────────────────────────────────────────────────────────────

FROM node:22-alpine AS build
WORKDIR /app

# ★package 파일만 먼저 복사★
#   소스만 고쳤을 때 npm ci 를 다시 돌리지 않게 하려는 캐시 전략.
COPY package.json package-lock.json ./

# npm install 이 아니라 npm ci 를 쓰는 이유:
#   lock 파일 그대로 설치한다 → 빌드할 때마다 같은 버전이 들어간다.
#   devDependencies 도 설치해야 한다 (빌드에 tsc·vite 가 필요).
RUN npm ci

COPY . .

# package.json 의 build = "tsc -b && vite build"
# 타입 오류가 있으면 여기서 빌드가 멈춘다 — 의도된 동작이다.
RUN npm run build


FROM nginx:alpine

# Vite 의 출력 디렉터리는 dist
COPY --from=build /app/dist /usr/share/nginx/html

# 기본 설정을 우리 것으로 교체 (SPA 라우팅 + /api 프록시)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
