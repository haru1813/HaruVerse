<script setup lang="ts">
/**
 * 회원 관리.
 *
 * <p><b>★이메일은 기본적으로 가린다★</b>
 * 관리자만 보는 화면이지만, 관리 도구는 화면 공유·스크린샷을 하게 되는 자리다.
 * 식별에 필요한 만큼만 남기고, 전체가 필요할 때만 토글로 펼친다.
 *
 * <p>권한 변경 버튼을 감추는 것으로 자물쇠를 대신하지 않는다.
 * 진짜 방어는 백엔드(AdminMemberService)에 있고, 여기서는 <b>왜 안 되는지</b>를 알려준다.
 */
import { computed, onMounted, ref, watch } from "vue";
import { api, ApiError } from "../lib/api";
import { readIdentity } from "../lib/auth";

type MemberRow = {
  id: number;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN";
  createdAt: string;
};

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
};

const rows = ref<MemberRow[]>([]);
const total = ref(0);
const pageCount = ref(0);
const page = ref(1); // Vuetify 페이지네이션은 1부터, 스프링은 0부터
const keyword = ref("");
const loading = ref(false);
const error = ref("");
const notice = ref("");
const showEmails = ref(false);
/** 지금 권한을 바꾸는 중인 회원 id */
const changing = ref<number | null>(null);

const me = readIdentity();

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const data = await api<Page<MemberRow>>("/api/admin/members", {
      params: { keyword: keyword.value, page: page.value - 1, size: 20 },
    });
    rows.value = data.content;
    total.value = data.totalElements;
    pageCount.value = data.totalPages;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "회원 목록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(page, load);

/** 검색 — 페이지를 1로 되돌리고 다시 불러온다 */
function search() {
  if (page.value !== 1) {
    page.value = 1; // watch 가 load 를 부른다
  } else {
    load();
  }
}

const adminCount = computed(() => rows.value.filter((r) => r.role === "ADMIN").length);

function isMe(row: MemberRow): boolean {
  return me?.email === row.email;
}

/** 이메일 표시 — 가릴 때는 앞 두 글자와 도메인만 */
function maskEmail(email: string): string {
  if (showEmails.value) return email;

  const [name, domain] = email.split("@");
  if (!domain) return "•".repeat(email.length);

  const head = name.slice(0, 2);
  return `${head}${"•".repeat(Math.max(name.length - 2, 2))}@${domain}`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

async function changeRole(row: MemberRow) {
  const next = row.role === "ADMIN" ? "USER" : "ADMIN";
  const label = next === "ADMIN" ? "관리자로 올립니다" : "일반 회원으로 내립니다";

  if (!window.confirm(`${row.nickname} 님을 ${label}. 계속할까요?`)) return;

  changing.value = row.id;
  error.value = "";
  notice.value = "";

  try {
    await api<MemberRow>(`/api/admin/members/${row.id}/role`, {
      method: "PATCH",
      body: { role: next },
    });
    notice.value = `${row.nickname} 님의 권한을 ${next} 로 바꿨습니다.`;
    await load();
  } catch (e) {
    // 자물쇠에 걸리면 409 가 온다 — 서버가 준 이유를 그대로 보여준다
    error.value = e instanceof ApiError ? e.message : "권한을 바꾸지 못했습니다.";
  } finally {
    changing.value = null;
  }
}
</script>

<template>
  <div>
    <h1 class="page-title">회원</h1>
    <p class="page-sub">
      전체 {{ total }}명 · 이 페이지의 관리자 {{ adminCount }}명
    </p>

    <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable
      @click:close="error = ''">
      {{ error }}
    </v-alert>
    <v-alert v-if="notice" type="success" variant="tonal" density="compact" class="mb-4" closable
      @click:close="notice = ''">
      {{ notice }}
    </v-alert>

    <v-card class="pa-4 mb-4">
      <div class="toolbar">
        <v-text-field
          v-model="keyword"
          label="이메일 · 닉네임 검색"
          prepend-inner-icon="mdi-magnify"
          clearable
          hide-details
          density="comfortable"
          @keyup.enter="search"
          @click:clear="search"
        />
        <v-btn color="primary" :loading="loading" @click="search">검색</v-btn>
        <v-switch
          v-model="showEmails"
          label="이메일 전체 보기"
          color="primary"
          density="compact"
          hide-details
          class="mask-switch"
        />
      </div>
    </v-card>

    <v-card>
      <v-table density="comfortable">
        <thead>
          <tr>
            <th class="num">ID</th>
            <th>닉네임</th>
            <th>이메일</th>
            <th>권한</th>
            <th>가입일</th>
            <th class="text-right">권한 변경</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && rows.length === 0">
            <td colspan="6" class="empty">불러오는 중…</td>
          </tr>
          <tr v-else-if="rows.length === 0">
            <td colspan="6" class="empty">회원이 없습니다.</td>
          </tr>

          <tr v-for="row in rows" :key="row.id">
            <td class="num">{{ row.id }}</td>
            <td>
              {{ row.nickname }}
              <v-chip v-if="isMe(row)" size="x-small" variant="tonal" color="primary" class="ml-1">
                나
              </v-chip>
            </td>
            <td class="email">{{ maskEmail(row.email) }}</td>
            <td>
              <v-chip
                size="small"
                variant="tonal"
                :color="row.role === 'ADMIN' ? 'primary' : undefined"
              >
                {{ row.role }}
              </v-chip>
            </td>
            <td class="date">{{ formatDate(row.createdAt) }}</td>
            <td class="text-right">
              <!-- 자기 자신은 서버가 막는다. 눌러서 실패하게 두느니 이유를 먼저 보여준다 -->
              <span v-if="isMe(row)" class="locked">본인 계정은 변경 불가</span>
              <v-btn
                v-else
                size="small"
                variant="outlined"
                :loading="changing === row.id"
                :disabled="changing !== null"
                @click="changeRole(row)"
              >
                {{ row.role === "ADMIN" ? "일반 회원으로" : "관리자로" }}
              </v-btn>
            </td>
          </tr>
        </tbody>
      </v-table>

      <div v-if="pageCount > 1" class="pager">
        <v-pagination v-model="page" :length="pageCount" density="comfortable" total-visible="7" />
      </div>
    </v-card>

    <p class="footnote">
      마지막 관리자는 강등할 수 없습니다 — 콘솔에 들어갈 사람이 없어지기 때문입니다.
      다른 관리자를 먼저 지정하세요.
    </p>
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
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar .v-text-field {
  flex: 1 1 260px;
}
.mask-switch {
  flex: 0 0 auto;
}
.num {
  font-variant-numeric: tabular-nums;
  color: #94a3b8;
  width: 70px;
}
.email {
  font-family: ui-monospace, "SFMono-Regular", Menlo, monospace;
  font-size: 13px;
}
.date {
  font-variant-numeric: tabular-nums;
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}
.locked {
  font-size: 12px;
  color: #94a3b8;
}
.empty {
  text-align: center;
  color: #94a3b8;
  padding: 28px 0;
}
.pager {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  border-top: 1px solid #eef2f7;
}
.footnote {
  font-size: 12.5px;
  color: #64748b;
  margin: 14px 0 0;
  line-height: 1.65;
}
</style>
