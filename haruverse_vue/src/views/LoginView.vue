<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "../lib/api";
import { clearToken, isAdmin, setToken, type LoginResponse } from "../lib/auth";

const route = useRoute();
const router = useRouter();

const email = ref("");
const password = ref("");
const loading = ref(false);
const error = ref("");

// 가드가 '관리자가 아니라서' 돌려보낸 경우 (router/index.ts)
const denied = ref(route.query.denied === "1");

async function submit() {
  if (loading.value) return;

  loading.value = true;
  error.value = "";
  denied.value = false;

  try {
    // anonymous — 만료된 토큰이 남아 있어도 로그인 요청이 401로 튕기지 않게 한다
    const result = await api<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: { email: email.value, password: password.value },
      anonymous: true,
    });

    setToken(result.token);

    // ★로그인 성공 ≠ 입장 허가★
    //   백엔드는 일반 회원에게도 정상적으로 토큰을 발급한다.
    //   관리자 콘솔에 그 토큰을 남겨둘 이유가 없으므로 즉시 버린다.
    if (!isAdmin()) {
      clearToken();
      error.value = "이 계정에는 관리자 권한이 없습니다.";
      return;
    }

    // 토큰 만료로 튕겨 왔다면 하던 자리로 돌려보낸다
    const redirect = route.query.redirect;
    await router.replace(typeof redirect === "string" ? redirect : "/");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "로그인에 실패했습니다.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <v-app>
    <v-main class="login-ground">
      <v-container class="fill-height">
        <v-row justify="center" align="center">
          <v-col cols="12" sm="8" md="5" lg="4">
            <div class="text-center mb-8">
              <h1 class="brand">Haru<span>Verse</span></h1>
              <p class="brand-sub">관리자 콘솔</p>
            </div>

            <v-card class="pa-6">
              <v-alert v-if="denied" type="warning" variant="tonal" density="compact" class="mb-4">
                관리자 권한이 있는 계정으로 로그인해 주세요.
              </v-alert>

              <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4">
                {{ error }}
              </v-alert>

              <v-form @submit.prevent="submit">
                <v-text-field
                  v-model="email"
                  label="이메일"
                  type="email"
                  autocomplete="username"
                  autofocus
                  class="mb-2"
                />
                <v-text-field
                  v-model="password"
                  label="비밀번호"
                  type="password"
                  autocomplete="current-password"
                  class="mb-4"
                />
                <v-btn
                  type="submit"
                  color="primary"
                  size="large"
                  block
                  :loading="loading"
                  :disabled="!email || !password"
                >
                  로그인
                </v-btn>
              </v-form>
            </v-card>

            <p class="notice">
              이 화면은 HaruVerse 운영자 전용입니다.
            </p>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<style scoped>
.login-ground {
  background: #1b2a4a;
}

.brand {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: #fff;
  margin: 0;
}
.brand span {
  color: #38bdf8;
}
.brand-sub {
  color: #9db2d6;
  font-size: 14px;
  margin: 4px 0 0;
}
.notice {
  text-align: center;
  color: #7f95bb;
  font-size: 12.5px;
  margin-top: 20px;
}
</style>
