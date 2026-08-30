<script setup lang="ts">
/**
 * 대시보드 — 3단계에서 통계로 채운다.
 *
 * <p>지금 이 화면이 하는 일은 하나다: <b>토큰이 실제로 백엔드에서 통하는지 확인</b>.
 * 라우터 가드는 토큰을 서명 검증 없이 읽으므로, 가드를 통과했다는 것이
 * 곧 API가 받아준다는 뜻은 아니다. 인증이 필요한 API를 한 번 불러
 * 진짜로 로그인되었는지 눈으로 확인한다.
 */
import { onMounted, ref } from "vue";
import { api } from "../lib/api";
import { readIdentity } from "../lib/auth";

type MemberResponse = { id: number; email: string; nickname: string };

const me = ref<MemberResponse | null>(null);
const error = ref("");
const loading = ref(true);

const identity = readIdentity();

onMounted(async () => {
  try {
    me.value = await api<MemberResponse>("/api/members/me");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "회원 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div>
    <h1 class="page-title">대시보드</h1>
    <p class="page-sub">HaruVerse 운영 현황</p>

    <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4">
      {{ error }}
    </v-alert>

    <v-card class="pa-5 mb-4" max-width="520">
      <div class="label">연결 확인</div>

      <v-progress-circular v-if="loading" indeterminate size="22" width="2" color="primary" />

      <template v-else-if="me">
        <div class="row-line">
          <span>계정</span>
          <b>{{ me.nickname }} ({{ me.email }})</b>
        </div>
        <div class="row-line">
          <span>권한</span>
          <v-chip size="small" color="primary" variant="tonal">{{ identity?.role }}</v-chip>
        </div>
        <div class="row-line">
          <span>토큰 만료</span>
          <b>{{ identity ? new Date(identity.expiresAt).toLocaleString("ko-KR") : "—" }}</b>
        </div>
        <p class="ok">백엔드 인증이 정상 동작합니다.</p>
      </template>
    </v-card>

    <v-card class="pa-5" max-width="520">
      <div class="label">다음 단계</div>
      <p class="next">
        통계 카드는 <b>3단계</b>에서 채웁니다 —
        작품·캐릭터·회원 건수, 한국어 제목 채움률,
        그리고 <b>ES 색인 드리프트 감지</b>.
      </p>
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
  margin-bottom: 12px;
}
.row-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 7px 0;
  border-bottom: 1px solid #eef2f7;
  font-size: 14px;
}
.row-line:last-of-type {
  border-bottom: none;
}
.row-line span {
  color: #64748b;
}
.ok {
  margin: 14px 0 0;
  font-size: 13.5px;
  color: #047857;
}
.next {
  margin: 0;
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
}
</style>
