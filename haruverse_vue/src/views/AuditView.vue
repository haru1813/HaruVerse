<script setup lang="ts">
/**
 * 감사 로그 — 관리자가 무엇을 지우고 누구 권한을 바꿨는지.
 *
 * <p><b>★삭제 버튼이 없다★</b>
 * 관리자가 자기 흔적을 지울 수 있으면 감사 로그가 아니다.
 * 백엔드에도 삭제 경로를 만들지 않았다 — 화면에서 감추는 것과는 다른 이야기다.
 *
 * <p>기록은 <b>실제로 커밋된 일</b>만 담는다. 자물쇠에 막힌 요청이나
 * 롤백된 삭제는 남지 않는다(삭제와 같은 트랜잭션에서 기록하기 때문).
 */
import { onMounted, ref, watch } from "vue";
import { api } from "../lib/api";

type AuditRow = {
  id: number;
  actorEmail: string;
  action: "DELETE_POST" | "DELETE_COMMENT" | "CHANGE_ROLE";
  actionLabel: string;
  targetId: number | null;
  summary: string;
  createdAt: string;
};

type PageResult<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
};

const rows = ref<AuditRow[]>([]);
const total = ref(0);
const pageCount = ref(0);
const page = ref(1);
const loading = ref(false);
const error = ref("");

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const data = await api<PageResult<AuditRow>>("/api/admin/audit-logs", {
      params: { page: page.value - 1, size: 30 },
    });
    rows.value = data.content;
    total.value = data.totalElements;
    pageCount.value = data.totalPages;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "감사 로그를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(page, load);

/** 되돌릴 수 없는 삭제는 붉게, 권한 변경은 파랗게 */
function actionColor(action: AuditRow["action"]): string {
  return action === "CHANGE_ROLE" ? "primary" : "error";
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ko-KR", {
    year: "2-digit",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}
</script>

<template>
  <div>
    <h1 class="page-title">감사 로그</h1>
    <p class="page-sub">관리자 행위 기록 {{ total }}건 · 최근 순</p>

    <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4">
      {{ error }}
    </v-alert>

    <v-card>
      <v-table density="comfortable">
        <thead>
          <tr>
            <th class="date">시각</th>
            <th class="act">행위</th>
            <th>대상</th>
            <th class="who">실행자</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && rows.length === 0">
            <td colspan="4" class="empty">불러오는 중…</td>
          </tr>
          <tr v-else-if="rows.length === 0">
            <td colspan="4" class="empty">
              아직 기록이 없습니다. 글·댓글을 지우거나 권한을 바꾸면 여기 남습니다.
            </td>
          </tr>

          <tr v-for="row in rows" :key="row.id">
            <td class="date">{{ formatDate(row.createdAt) }}</td>
            <td class="act">
              <v-chip size="small" variant="tonal" :color="actionColor(row.action)">
                {{ row.actionLabel }}
              </v-chip>
            </td>
            <td class="summary">
              {{ row.summary }}
              <span v-if="row.targetId" class="tid">#{{ row.targetId }}</span>
            </td>
            <td class="who">{{ row.actorEmail }}</td>
          </tr>
        </tbody>
      </v-table>

      <div v-if="pageCount > 1" class="pager">
        <v-pagination v-model="page" :length="pageCount" density="comfortable" total-visible="7" />
      </div>
    </v-card>

    <p class="footnote">
      기록은 지울 수 없습니다. 삭제된 글·댓글의 내용은 여기 남은 요약이 유일한 흔적입니다.
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
.date {
  font-variant-numeric: tabular-nums;
  color: #64748b;
  font-size: 12.5px;
  white-space: nowrap;
  width: 160px;
}
.act {
  width: 110px;
  white-space: nowrap;
}
.summary {
  font-size: 13.5px;
  line-height: 1.6;
  color: #0f1a2e;
}
.tid {
  color: #94a3b8;
  font-size: 12px;
  margin-left: 6px;
  font-variant-numeric: tabular-nums;
}
.who {
  font-family: ui-monospace, "SFMono-Regular", Menlo, monospace;
  font-size: 12.5px;
  color: #475569;
  width: 220px;
  white-space: nowrap;
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
}
</style>
