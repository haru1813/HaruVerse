<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { clearToken, readIdentity } from "../lib/auth";

const router = useRouter();
const drawer = ref(true);

const identity = computed(() => readIdentity());

/**
 * 메뉴.
 *
 * <p>아직 만들지 않은 화면도 자리를 잡아 둔다(ready: false).
 * 관리 도구는 '무엇을 할 수 있는 도구인지'가 한눈에 보여야 하고,
 * 단계별로 붙여 나가는 동안 진행 상황이 그대로 드러난다.
 */
const menu = [
  { title: "대시보드", icon: "mdi-view-dashboard-outline", to: "/", ready: true },
  { title: "수집 · 색인", icon: "mdi-cloud-download-outline", to: "/collect", ready: false },
  { title: "회원", icon: "mdi-account-multiple-outline", to: "/members", ready: false },
  { title: "커뮤니티", icon: "mdi-forum-outline", to: "/community", ready: false },
];

function logout() {
  clearToken();
  router.replace({ name: "login" });
}
</script>

<template>
  <v-app>
    <v-app-bar color="navy" flat density="comfortable">
      <v-app-bar-nav-icon @click="drawer = !drawer" />

      <v-app-bar-title>
        <span class="brand">Haru<span class="accent">Verse</span></span>
        <span class="tag">관리자</span>
      </v-app-bar-title>

      <v-spacer />

      <span class="who d-none d-sm-inline">{{ identity?.email }}</span>
      <v-btn variant="text" prepend-icon="mdi-logout" @click="logout">로그아웃</v-btn>
    </v-app-bar>

    <v-navigation-drawer v-model="drawer" width="228">
      <v-list density="compact" nav>
        <v-list-item
          v-for="item in menu"
          :key="item.title"
          :prepend-icon="item.icon"
          :to="item.ready ? item.to : undefined"
          :disabled="!item.ready"
          :title="item.title"
        >
          <template v-if="!item.ready" #append>
            <span class="soon">준비 중</span>
          </template>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>

    <v-main>
      <v-container fluid class="pa-6">
        <router-view />
      </v-container>
    </v-main>
  </v-app>
</template>

<style scoped>
.brand {
  font-weight: 800;
  letter-spacing: -0.5px;
}
.accent {
  color: #38bdf8;
}
.tag {
  font-size: 12px;
  font-weight: 600;
  margin-left: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.18);
  color: #9fdcfb;
  vertical-align: middle;
}
.who {
  font-size: 13px;
  color: #b9c9e4;
  margin-right: 12px;
}
.soon {
  font-size: 10.5px;
  color: #94a3b8;
  border: 1px solid #dbe2ec;
  border-radius: 4px;
  padding: 1px 5px;
}
</style>
