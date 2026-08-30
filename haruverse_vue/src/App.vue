<script setup lang="ts">
// 최상위 — 라우팅과 '인증 만료' 처리만 맡는다. 화면은 각 라우트가 그린다.
//
// ★401을 여기서 받는 이유★
//   lib/api.ts 는 401을 만나면 토큰을 버리고 이벤트를 쏜다.
//   api 가 라우터를 직접 부르면 router → views → api → router 로 순환 참조가 생긴다.
//   이벤트를 여기서 받아 라우팅으로 옮기면 그 고리가 끊긴다.
import { onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { UNAUTHORIZED_EVENT } from "./lib/api";

const router = useRouter();
const route = useRoute();

function onUnauthorized() {
  if (route.name === "login") return; // 이미 로그인 화면이면 그대로 둔다
  router.replace({ name: "login", query: { redirect: route.fullPath } });
}

onMounted(() => window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized));
onUnmounted(() => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized));
</script>

<template>
  <router-view />
</template>
