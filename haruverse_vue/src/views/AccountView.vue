<script setup lang="ts">
/**
 * 내 계정 — 비밀번호 변경.
 *
 * <p>관리자 콘솔이 인터넷에 열리기 전에 비밀번호를 바꿀 수단이 필요해 만든 화면이다.
 * 관리자가 <b>남의</b> 비밀번호를 재설정하는 기능은 일부러 두지 않았다 —
 * 그게 있으면 아무 계정에나 들어갈 수 있고, 나중에 누가 한 일인지 가릴 수 없다.
 */
import { computed, onMounted, ref } from "vue";
import { api, ApiError } from "../lib/api";
import { clearToken, readIdentity } from "../lib/auth";
import { useRouter } from "vue-router";

type MemberResponse = { id: number; email: string; nickname: string };

const router = useRouter();
const identity = readIdentity();

const me = ref<MemberResponse | null>(null);
const current = ref("");
const next = ref("");
const confirm = ref("");
const show = ref(false);
const saving = ref(false);
const error = ref("");
const done = ref(false);

onMounted(async () => {
  try {
    me.value = await api<MemberResponse>("/api/members/me");
  } catch {
    // 계정 정보를 못 읽어도 비밀번호 변경 자체는 가능하다 — 조용히 넘어간다
  }
});

/** 새 비밀번호 강도 — 길이만 본다 (규칙을 늘리면 예측 가능한 형태로 수렴한다) */
const strength = computed(() => {
  const n = next.value.length;
  if (n === 0) return { value: 0, color: "grey", label: "" };
  if (n < 8) return { value: 25, color: "error", label: "8자 이상이어야 합니다" };
  if (n < 12) return { value: 55, color: "warning", label: "사용 가능 — 12자 이상을 권합니다" };
  if (n < 16) return { value: 80, color: "success", label: "좋습니다" };
  return { value: 100, color: "success", label: "충분히 깁니다" };
});

const mismatch = computed(() => confirm.value.length > 0 && next.value !== confirm.value);

const canSubmit = computed(
  () =>
    current.value.length > 0 &&
    next.value.length >= 8 &&
    next.value === confirm.value &&
    next.value !== current.value,
);

async function submit() {
  if (!canSubmit.value || saving.value) return;

  saving.value = true;
  error.value = "";

  try {
    await api<void>("/api/members/me/password", {
      method: "PATCH",
      body: { currentPassword: current.value, newPassword: next.value },
    });
    done.value = true;
  } catch (e) {
    // 서버가 준 이유를 그대로 보여준다 ("현재 비밀번호가 올바르지 않습니다" 등)
    error.value = e instanceof ApiError ? e.message : "비밀번호를 바꾸지 못했습니다.";
  } finally {
    saving.value = false;
  }
}

/**
 * 비밀번호를 바꾼 뒤에는 다시 로그인시킨다.
 *
 * <p>기존 토큰은 여전히 유효하다(JWT 는 서버가 회수할 수 없다).
 * 그대로 두면 "바꿨는데 예전 비밀번호로 받은 토큰이 계속 통하는" 상태가 된다.
 * 적어도 이 브라우저에서는 새 비밀번호로 다시 받게 한다.
 */
function relogin() {
  clearToken();
  router.replace({ name: "login" });
}
</script>

<template>
  <div>
    <h1 class="page-title">내 계정</h1>
    <p class="page-sub">{{ me?.nickname ?? identity?.email }} · {{ identity?.role }}</p>

    <v-card class="pa-6" max-width="520">
      <template v-if="done">
        <div class="text-center py-4">
          <v-icon icon="mdi-check-circle" color="success" size="44" />
          <h2 class="done-title">비밀번호를 바꿨습니다</h2>
          <p class="done-note">
            보안을 위해 다시 로그인해 주세요. 지금 가진 토큰은 예전 비밀번호로 받은 것입니다.
          </p>
          <v-btn color="primary" class="mt-4" @click="relogin">다시 로그인</v-btn>
        </div>
      </template>

      <template v-else>
        <div class="label">비밀번호 변경</div>

        <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4">
          {{ error }}
        </v-alert>

        <v-form @submit.prevent="submit">
          <v-text-field
            v-model="current"
            label="현재 비밀번호"
            :type="show ? 'text' : 'password'"
            autocomplete="current-password"
            class="mb-2"
          />

          <v-text-field
            v-model="next"
            label="새 비밀번호"
            :type="show ? 'text' : 'password'"
            autocomplete="new-password"
            hide-details="auto"
          />
          <v-progress-linear
            v-if="next.length > 0"
            :model-value="strength.value"
            :color="strength.color"
            height="4"
            rounded
            class="mt-2"
          />
          <p v-if="strength.label" class="strength" :class="strength.color">
            {{ strength.label }}
          </p>

          <v-text-field
            v-model="confirm"
            label="새 비밀번호 확인"
            :type="show ? 'text' : 'password'"
            autocomplete="new-password"
            :error="mismatch"
            :error-messages="mismatch ? '입력한 두 값이 다릅니다' : undefined"
            class="mt-3"
            hide-details="auto"
          />

          <v-switch
            v-model="show"
            label="비밀번호 표시"
            color="primary"
            density="compact"
            hide-details
            class="mt-2"
          />

          <v-btn
            type="submit"
            color="primary"
            size="large"
            block
            class="mt-4"
            :loading="saving"
            :disabled="!canSubmit"
          >
            변경
          </v-btn>
        </v-form>

        <p class="hint">
          이 콘솔은 인터넷에 열려 있습니다. 다른 곳에서 쓰지 않는,
          <b>충분히 긴</b> 비밀번호를 쓰세요.
        </p>
      </template>
    </v-card>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin: 0;
}
.page-sub {
  color: #64748b;
  font-size: 14px;
  margin: 2px 0 22px;
}
.label {
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #94a3b8;
  font-weight: 600;
  margin-bottom: 16px;
}
.strength {
  font-size: 12.5px;
  margin: 6px 0 0;
}
.strength.error {
  color: #b91c1c;
}
.strength.warning {
  color: #b45309;
}
.strength.success {
  color: #047857;
}
.hint {
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.65;
  margin: 18px 0 0;
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
}
.done-title {
  font-size: 18px;
  font-weight: 700;
  margin: 12px 0 6px;
}
.done-note {
  font-size: 13.5px;
  color: #64748b;
  line-height: 1.7;
  margin: 0;
}
</style>
