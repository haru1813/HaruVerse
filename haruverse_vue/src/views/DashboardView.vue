<script setup lang="ts">
/**
 * 대시보드 — HaruVerse 운영 현황.
 *
 * <p><b>★숫자를 늘어놓는 화면이 아니다★</b>
 * 관리자가 알고 싶은 건 "몇 개인가"보다 <b>"뭐가 어긋났는가"</b>다.
 * 그래서 단순 건수는 작게 늘어놓고, <b>비교해야 의미가 생기는 값</b>은
 * 진행률·경고와 함께 위쪽에 크게 둔다.
 */
import { computed, onMounted, ref } from "vue";
import { api } from "../lib/api";

type Stats = {
  works: number;
  anime: number;
  games: number;
  titleKoFilled: number;
  characters: number;
  animeWithCharacters: number;
  voiceActors: number;
  studios: number;
  members: number;
  admins: number;
  posts: number;
  comments: number;
  /** null = Elasticsearch에 연결하지 못함 (0과 뜻이 다르다) */
  indexed: number | null;
  indexDrift: boolean;
};

const stats = ref<Stats | null>(null);
const error = ref("");
const loading = ref(true);

async function load() {
  loading.value = true;
  error.value = "";
  try {
    stats.value = await api<Stats>("/api/admin/stats");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "통계를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

onMounted(load);

/** 한국어 제목 채움률 — 분모는 애니(TMDB에 게임은 없다) */
const titleKoPercent = computed(() => {
  if (!stats.value || stats.value.anime === 0) return 0;
  return Math.round((stats.value.titleKoFilled / stats.value.anime) * 100);
});

/**
 * 캐릭터가 비어 있는 애니 수.
 *
 * ★분모는 애니다★ 캐릭터는 Jikan 에서만 오므로 게임은 애초에 대상이 아니다.
 * 전체 작품을 분모로 쓰면 게임 100여 편이 섞여 실제보다 나쁘게 보인다.
 */
const animeWithoutCharacters = computed(() => {
  if (!stats.value) return 0;
  return stats.value.anime - stats.value.animeWithCharacters;
});

const characterPercent = computed(() => {
  if (!stats.value || stats.value.anime === 0) return 0;
  return Math.round((stats.value.animeWithCharacters / stats.value.anime) * 100);
});

/** 색인 상태 — 세 가지다: 연결 실패 / 어긋남 / 정상 */
const indexState = computed<{ tone: "error" | "warning" | "success"; text: string }>(() => {
  const s = stats.value;
  if (!s) return { tone: "warning", text: "—" };

  if (s.indexed === null) {
    return {
      tone: "error",
      text: "Elasticsearch에 연결하지 못했습니다. 검색은 DB 조회로 동작합니다.",
    };
  }
  if (s.indexDrift) {
    const diff = s.indexed - s.works;
    return {
      tone: "warning",
      text: `색인이 DB와 어긋나 있습니다 (${diff > 0 ? "+" : ""}${diff}건). 재색인이 필요합니다.`,
    };
  }
  return { tone: "success", text: "색인이 DB와 일치합니다." };
});

/** 아래쪽에 늘어놓는 단순 건수 */
const counts = computed(() => {
  const s = stats.value;
  if (!s) return [];
  return [
    { label: "애니메이션", value: s.anime, icon: "mdi-television-classic" },
    { label: "게임", value: s.games, icon: "mdi-controller" },
    { label: "캐릭터", value: s.characters, icon: "mdi-account-star-outline" },
    { label: "성우", value: s.voiceActors, icon: "mdi-microphone-outline" },
    { label: "제작사", value: s.studios, icon: "mdi-domain" },
    { label: "회원", value: s.members, icon: "mdi-account-multiple-outline" },
    { label: "관리자", value: s.admins, icon: "mdi-shield-account-outline" },
    { label: "게시글", value: s.posts, icon: "mdi-note-text-outline" },
    { label: "댓글", value: s.comments, icon: "mdi-comment-outline" },
  ];
});
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-1">
      <h1 class="page-title">대시보드</h1>
      <v-btn
        variant="text"
        size="small"
        prepend-icon="mdi-refresh"
        :loading="loading"
        @click="load"
      >
        새로고침
      </v-btn>
    </div>
    <p class="page-sub">HaruVerse 운영 현황</p>

    <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4">
      {{ error }}
    </v-alert>

    <div v-if="loading && !stats" class="py-10 text-center">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <template v-else-if="stats">
      <!-- ── 살펴야 하는 것 ── -->
      <div class="watch-grid mb-6">
        <!-- 검색 색인 -->
        <v-card class="pa-5">
          <div class="label">검색 색인</div>
          <div class="big-row">
            <span class="big">{{ stats.indexed ?? "—" }}</span>
            <span class="big-sub">/ {{ stats.works }} 작품</span>
          </div>
          <v-alert
            :type="indexState.tone"
            variant="tonal"
            density="compact"
            class="mt-3 state-note"
          >
            {{ indexState.text }}
          </v-alert>
        </v-card>

        <!-- 한국어 제목 -->
        <v-card class="pa-5">
          <div class="label">한국어 제목</div>
          <div class="big-row">
            <span class="big">{{ stats.titleKoFilled }}</span>
            <span class="big-sub">/ {{ stats.anime }} 애니 · {{ titleKoPercent }}%</span>
          </div>
          <v-progress-linear
            :model-value="titleKoPercent"
            color="secondary"
            height="6"
            rounded
            class="mt-3"
          />
          <p class="note">
            채우지 못한 {{ stats.anime - stats.titleKoFilled }}편은 TMDB에 대응 작품이 없다.
            틀린 제목을 붙이느니 비워 둔다.
          </p>
        </v-card>

        <!-- 캐릭터 -->
        <v-card class="pa-5">
          <div class="label">캐릭터가 있는 애니</div>
          <div class="big-row">
            <span class="big">{{ stats.animeWithCharacters }}</span>
            <span class="big-sub">/ {{ stats.anime }} 애니 · {{ characterPercent }}%</span>
          </div>
          <v-progress-linear
            :model-value="characterPercent"
            :color="characterPercent < 30 ? 'warning' : 'secondary'"
            height="6"
            rounded
            class="mt-3"
          />
          <p class="note">
            {{ animeWithoutCharacters }}편이 비어 있다. Jikan이 504를 자주 돌려주는 탓이다.
            게임은 캐릭터 수집 대상이 아니라 분모에서 뺐다.
          </p>
        </v-card>
      </div>

      <!-- ── 단순 건수 ── -->
      <h2 class="group-title">건수</h2>
      <div class="count-grid">
        <v-card v-for="c in counts" :key="c.label" class="pa-4 count-card">
          <v-icon :icon="c.icon" size="18" class="count-icon" />
          <div>
            <div class="count-value">{{ c.value.toLocaleString("ko-KR") }}</div>
            <div class="count-label">{{ c.label }}</div>
          </div>
        </v-card>
      </div>
    </template>
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
  margin-bottom: 10px;
}
.watch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 14px;
}
.big-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.big {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  color: #0f1a2e;
}
.big-sub {
  font-size: 13px;
  color: #64748b;
}
.state-note {
  font-size: 12.5px;
}
.note {
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.65;
  margin: 10px 0 0;
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
.count-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}
.count-card {
  display: flex;
  align-items: center;
  gap: 12px;
}
.count-icon {
  color: #94a3b8;
}
.count-value {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  color: #0f1a2e;
}
.count-label {
  font-size: 12.5px;
  color: #64748b;
}
</style>
