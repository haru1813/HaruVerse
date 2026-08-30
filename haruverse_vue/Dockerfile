# ─────────────────────────────────────────────────────────────
# HaruVerse 관리자 콘솔 (Vue 3 + Vite + TypeScript + Vuetify)
#
# 서비스 프론트(haruverse_react)와 같은 2단계 빌드다.
#   ① build — Node 로 정적 파일을 만든다
#   ② run   — Nginx 로 그 파일만 서빙한다
# 최종 이미지에 Node 도 node_modules 도 남지 않는다.
# ─────────────────────────────────────────────────────────────

FROM node:22-alpine AS build
WORKDIR /app

# ★package 파일만 먼저 복사★ 소스만 고쳤을 때 npm ci 를 다시 돌리지 않게 하는 캐시 전략
COPY package.json package-lock.json ./

# npm install 이 아니라 npm ci — lock 파일 그대로 설치한다.
# devDependencies 도 필요하다(빌드에 vue-tsc·vite 가 쓰인다).
RUN npm ci

COPY . .

# package.json 의 build = "vue-tsc -b && vite build"
# 타입 오류가 있으면 여기서 빌드가 멈춘다 — 의도된 동작이다.
RUN npm run build


FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# nginx.conf 가 location 마다 include 하는 공통 헤더
COPY security-headers.conf /etc/nginx/security-headers.conf
# limit_req_zone 은 http 블록에만 쓸 수 있어 conf.d 에 따로 둔다
COPY rate-limit.conf /etc/nginx/conf.d/00-rate-limit.conf

EXPOSE 80
