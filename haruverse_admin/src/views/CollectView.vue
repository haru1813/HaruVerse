<script setup lang="ts">
/**
 * 수집 · 색인 운영 콘솔.
 *
 * <p>그동안 터미널에서 curl 로 치던 명령을 화면으로 옮긴 것이다.
 * 백엔드는 손대지 않았다 — 이미 있는 ADMIN API 10개를 그대로 부른다.
 *
 * <p><b>★한 번에 하나만 실행한다★</b>
 * 수집은 외부 API(Jikan·RAWG·TMDB)를 페이지 단위로 도는 작업이라 몇 분씩 걸리고,
 * 그동안 상대 서버의 쿼터를 쓴다. 두 개를 동시에 돌리면 쿼터가 두 배로 나가고
 * 같은 작품을 양쪽에서 쓰다 충돌한다. 실행 중에는 모든 버튼을 잠근다.
 */
import { computed, onUnmounted, reactive, ref } from "vue";
import { api, ApiError } from "../lib/api";
import { GOOD_KEYS, RESULT_LABELS, TASKS, WARN_KEYS, type Field, type Task } from "../lib/collectTasks";

type Result = {
  ok: boolean;
  /** 성공 시 응답 본문 (키-값을 그대로 훑어 표시한다) */
  data?: Record<string, unknown>;
  error?: string;
  /** 걸린 시간 (초) */
  seconds: number;
  at: Date;
};

/** 실행 중인 작업 id — null이면 유휴. 전역 잠금 역할을 겸한다 */
const running = ref<string | null>(null);
/** 실행 중인 작업의 경과 초 */
const elapsed = ref(0);
const results = reactive<Record<string, Result>>({});

/** 작업별 입력값 — 정의의 기본값으로 채워 시작한다 */
const values = reactive<Record<string, Record<string, string | number | boolean>>>(
  Object.fromEntries(
    TASKS.map((t) => [t.id, Object.fromEntries(t.fields.map((f) => [f.key, f.default]))]),
  ),
);

const groups = computed(() => {
  const map = new Map<string, Task[]>();
  for (const task of TASKS) {
    const list = map.get(task.group) ?? [];
    list.push(task);
    map.set(task.group, list);
  }
  return [...map.entries()];
});

let timer: number | undefined;

/**
 * ★실행 중 이탈 경고★
 * 수집은 요청이 끝날 때까지 브라우저가 붙들고 있는 동기 방식이다.
 * 탭을 닫으면 진행 상황을 볼 방법이 사라진다(작업 자체는 서버에서 계속 돈다).
 */
function guardUnload(e: BeforeUnloadEvent) {
  e.preventDefault();
  e.returnValue = "";
}

function stopTimer() {
  if (timer !== undefined) {
    clearInterval(timer);
    timer = undefined;
  }
  window.removeEventListener("beforeunload", guardUnload);
}

onUnmounted(stopTimer);

/** ids 같은 목록 입력이 비었는지 — 비면 백엔드가 400을 준다 */
function missingRequired(task: Task): string | null {
  for (const field of task.fields) {
    const value = values[task.id][field.key];
    if (field.type === "text" && String(value).trim() === "") {
      return `${field.label} 값을 입력해 주세요.`;
    }
    if (field.key === "workId" && Number(value) <= 0) {
      return "작품 ID를 입력해 주세요.";
    }
  }
  return null;
}

async function run(task: Task) {
  if (running.value) return; // 전역 잠금

  const missing = missingRequired(task);
  if (missing) {
    results[task.id] = { ok: false, error: missing, seconds: 0, at: new Date() };
    return;
  }

  if (task.confirm && !window.confirm(task.confirm)) return;

  running.value = task.id;
  elapsed.value = 0;
  delete results[task.id];

  const startedAt = Date.now();
  timer = window.setInterval(() => {
    elapsed.value = Math.floor((Date.now() - startedAt) / 1000);
  }, 1000);
  window.addEventListener("beforeunload", guardUnload);

  try {
    const data = await api<Record<string, unknown>>(task.path, {
      method: "POST",
      params: values[task.id],
    });
    results[task.id] = {
      ok: true,
      // 본문 없는 응답(204)도 성공으로 다룬다
      data: data ?? {},
      seconds: Math.round((Date.now() - startedAt) / 1000),
      at: new Date(),
    };
  } catch (e) {
    results[task.id] = {
      ok: false,
      error: e instanceof ApiError ? e.message : e instanceof Error ? e.message : "실행에 실패했습니다.",
      seconds: Math.round((Date.now() - startedAt) / 1000),
      at: new Date(),
    };
  } finally {
    running.value = null;
    stopTimer();
  }
}

/** 결과 한 칸의 색 — 실패 계열은 값이 있을 때만 붉게, 성과는 초록 */
function chipColor(key: string, value: unknown): string | undefined {
  const positive = typeof value === "number" ? value > 0 : value === true;
  if (WARN_KEYS.has(key)) return positive ? "warning" : undefined;
  if (GOOD_KEYS.has(key)) return positive ? "success" : undefined;
  return undefined;
}

function label(key: string): string {
  return RESULT_LABELS[key] ?? key;
}

function display(value: unknown): string {
  if (typeof value === "boolean") return value ? "예" : "아니오";
  return String(value);
}

/** 이 작업이 지금 잠겨 있는가 (다른 작업이 도는 중) */
function lockedByOther(task: Task): boolean {
  return running.value !== null && running.value !== task.id;
}

function fieldOptions(field: Field): string[] {
  return field.type === "select" ? field.options : [];
}
</script>

<template>
  <div>
    <h1 class="page-title">수집 · 색인</h1>
    <p class="page-sub">외부 API에서 데이터를 가져오고 검색 색인을 관리한다</p>

    <v-alert
      v-if="running"
      type="info"
      variant="tonal"
      density="comfortable"
      class="mb-5"
      :icon="false"
    >
      <div class="d-flex align-center ga-3">
        <v-progress-circular indeterminate size="20" width="2" />
        <span>
          <b>{{ TASKS.find((t) => t.id === running)?.title }}</b> 실행 중 —
          {{ elapsed }}초 경과. 끝날 때까지 다른 작업은 잠깁니다.
        </span>
      </div>
    </v-alert>

    <div v-for="[group, tasks] in groups" :key="group" class="mb-7">
      <h2 class="group-title">{{ group }}</h2>

      <div class="task-grid">
        <v-card v-for="task in tasks" :key="task.id" class="pa-5">
          <div class="d-flex align-start justify-space-between ga-2 mb-1">
            <h3 class="task-title">{{ task.title }}</h3>
            <v-chip v-if="task.slow" size="x-small" variant="tonal" color="warning">
              오래 걸림
            </v-chip>
          </div>
          <p class="task-desc">{{ task.description }}</p>

          <div v-if="task.fields.length" class="fields">
            <template v-for="field in task.fields" :key="field.key">
              <v-switch
                v-if="field.type === 'switch'"
                :model-value="values[task.id][field.key] === true"
                @update:model-value="values[task.id][field.key] = $event === true"
                :label="field.label"
                :hint="field.hint"
                :persistent-hint="!!field.hint"
                color="primary"
                density="compact"
                hide-details="auto"
                class="switch-field"
              />
              <v-select
                v-else-if="field.type === 'select'"
                :model-value="String(values[task.id][field.key] ?? '')"
                @update:model-value="values[task.id][field.key] = $event"
                :label="field.label"
                :items="fieldOptions(field)"
                :hint="field.hint"
                :persistent-hint="!!field.hint"
                hide-details="auto"
              />
              <!-- ★v-model 을 쓰지 않는다★
                   values 는 number·boolean·string 을 함께 담는데 VTextField 의
                   model-value 는 string 만 받는다. 문자열로 주고받고,
                   전송할 때 buildQuery 가 어차피 String() 으로 바꾸므로 동작은 같다. -->
              <v-text-field
                v-else
                :model-value="String(values[task.id][field.key] ?? '')"
                @update:model-value="values[task.id][field.key] = $event"
                :label="field.label"
                :type="field.type === 'number' ? 'number' : 'text'"
                :hint="field.hint"
                :persistent-hint="!!field.hint"
                hide-details="auto"
              />
            </template>
          </div>

          <v-btn
            color="primary"
            class="mt-4"
            block
            :loading="running === task.id"
            :disabled="lockedByOther(task)"
            @click="run(task)"
          >
            실행
          </v-btn>

          <!-- 결과 -->
          <div v-if="results[task.id]" class="result mt-4">
            <div class="result-head">
              <v-icon
                :icon="results[task.id].ok ? 'mdi-check-circle' : 'mdi-alert-circle'"
                :color="results[task.id].ok ? 'success' : 'error'"
                size="18"
              />
              <span>
                {{ results[task.id].ok ? "완료" : "실패" }} ·
                {{ results[task.id].seconds }}초 ·
                {{ results[task.id].at.toLocaleTimeString("ko-KR") }}
              </span>
            </div>

            <p v-if="results[task.id].error" class="result-error">
              {{ results[task.id].error }}
            </p>

            <div v-else class="result-chips">
              <v-chip
                v-for="(value, key) in results[task.id].data"
                :key="key"
                size="small"
                variant="tonal"
                :color="chipColor(String(key), value)"
              >
                {{ label(String(key)) }} <b class="ml-1">{{ display(value) }}</b>
              </v-chip>
            </div>
          </div>
        </v-card>
      </div>
    </div>
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
  margin: 2px 0 24px;
}
.group-title {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #475569;
  margin: 0 0 10px;
  padding-bottom: 7px;
  border-bottom: 1px solid #e2e8f0;
}
.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}
.task-title {
  font-size: 15.5px;
  font-weight: 600;
  margin: 0;
}
.task-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.65;
  margin: 0 0 14px;
}
.fields {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.switch-field {
  margin-top: -4px;
}
.result {
  border-top: 1px solid #eef2f7;
  padding-top: 12px;
}
.result-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #64748b;
  margin-bottom: 8px;
}
.result-error {
  font-size: 13px;
  color: #b91c1c;
  margin: 0;
  line-height: 1.6;
}
.result-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
