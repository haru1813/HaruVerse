<script setup lang="ts">
/**
 * 커뮤니티 관리 — 게시글·댓글 조회와 삭제.
 *
 * <p><b>★삭제가 되돌릴 수 없다★</b>
 * 이 프로젝트는 소프트 삭제를 쓰지 않는다. 글을 지우면 댓글·추천까지
 * 함께 DB 에서 사라지고 복구할 방법이 없다.
 * 그래서 확인 창에 <b>무엇을 지우는지</b>(제목·작성자·댓글 수)를 그대로 보여준다.
 * "정말 삭제하시겠습니까?" 만 띄우는 건 확인이 아니라 요식이다.
 */
import { computed, onMounted, ref, watch } from "vue";
import { api, ApiError } from "../lib/api";

type PostRow = {
  id: number;
  title: string;
  excerpt: string;
  authorNickname: string;
  workTitle: string;
  viewCount: number;
  commentCount: number;
  createdAt: string;
};

type CommentRow = {
  id: number;
  content: string;
  authorNickname: string;
  postId: number;
  postTitle: string;
  /** 답글이면 부모 댓글 id — 최상위면 null */
  parentId: number | null;
  createdAt: string;
};

type PageResult<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
};

const tab = ref<"posts" | "comments">("posts");
const keyword = ref("");
const page = ref(1);
const loading = ref(false);
const error = ref("");
const notice = ref("");
const deleting = ref<number | null>(null);

const posts = ref<PostRow[]>([]);
const comments = ref<CommentRow[]>([]);
const total = ref(0);
const pageCount = ref(0);

const isPosts = computed(() => tab.value === "posts");

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const path = isPosts.value ? "/api/admin/posts" : "/api/admin/comments";
    const data = await api<PageResult<PostRow | CommentRow>>(path, {
      params: { keyword: keyword.value, page: page.value - 1, size: 20 },
    });

    if (isPosts.value) posts.value = data.content as PostRow[];
    else comments.value = data.content as CommentRow[];

    total.value = data.totalElements;
    pageCount.value = data.totalPages;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "목록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
watch(page, load);
// 탭을 바꾸면 페이지·검색어를 초기화하고 다시 읽는다
watch(tab, () => {
  keyword.value = "";
  if (page.value !== 1) page.value = 1;
  else load();
});

function search() {
  if (page.value !== 1) page.value = 1;
  else load();
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("ko-KR", {
    year: "2-digit",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

async function removePost(row: PostRow) {
  // ★무엇이 함께 사라지는지 숫자로 보여준다★
  const extra = row.commentCount > 0 ? `\n댓글 ${row.commentCount}개도 함께 삭제됩니다.` : "";
  const ok = window.confirm(
    `다음 글을 삭제합니다. 되돌릴 수 없습니다.\n\n` +
      `제목: ${row.title}\n작성자: ${row.authorNickname}\n게시판: ${row.workTitle}${extra}`,
  );
  if (!ok) return;

  await run(row.id, `/api/admin/posts/${row.id}`, `"${row.title}" 글을 삭제했습니다.`);
}

async function removeComment(row: CommentRow) {
  const preview = row.content.length > 40 ? row.content.slice(0, 40) + "…" : row.content;
  const kind = row.parentId ? "답글" : "댓글";
  // 최상위 댓글을 지우면 딸린 답글도 함께 사라진다 — 그 사실을 미리 알린다
  const extra = row.parentId ? "" : "\n이 댓글에 달린 답글도 함께 삭제됩니다.";

  const ok = window.confirm(
    `다음 ${kind}을 삭제합니다. 되돌릴 수 없습니다.\n\n` +
      `내용: ${preview}\n작성자: ${row.authorNickname}\n원글: ${row.postTitle}${extra}`,
  );
  if (!ok) return;

  await run(row.id, `/api/admin/comments/${row.id}`, `${kind}을 삭제했습니다.`);
}

async function run(id: number, path: string, message: string) {
  deleting.value = id;
  error.value = "";
  notice.value = "";
  try {
    await api<void>(path, { method: "DELETE" });
    notice.value = message;
    await load();
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : "삭제하지 못했습니다.";
  } finally {
    deleting.value = null;
  }
}
</script>

<template>
  <div>
    <h1 class="page-title">커뮤니티</h1>
    <p class="page-sub">
      {{ isPosts ? "게시글" : "댓글" }} 전체 {{ total }}건
    </p>

    <v-alert v-if="error" type="error" variant="tonal" density="compact" class="mb-4" closable
      @click:close="error = ''">
      {{ error }}
    </v-alert>
    <v-alert v-if="notice" type="success" variant="tonal" density="compact" class="mb-4" closable
      @click:close="notice = ''">
      {{ notice }}
    </v-alert>

    <v-tabs v-model="tab" color="primary" class="mb-4">
      <v-tab value="posts" prepend-icon="mdi-note-text-outline">게시글</v-tab>
      <v-tab value="comments" prepend-icon="mdi-comment-outline">댓글</v-tab>
    </v-tabs>

    <v-card class="pa-4 mb-4">
      <div class="toolbar">
        <v-text-field
          v-model="keyword"
          :label="isPosts ? '제목 · 본문 · 작성자 검색' : '내용 · 작성자 검색'"
          prepend-inner-icon="mdi-magnify"
          clearable
          hide-details
          density="comfortable"
          @keyup.enter="search"
          @click:clear="search"
        />
        <v-btn color="primary" :loading="loading" @click="search">검색</v-btn>
      </div>
    </v-card>

    <v-card>
      <!-- ── 게시글 ── -->
      <v-table v-if="isPosts" density="comfortable">
        <thead>
          <tr>
            <th class="num">ID</th>
            <th>제목 · 본문</th>
            <th>작성자</th>
            <th>게시판</th>
            <th class="num">조회</th>
            <th class="num">댓글</th>
            <th>작성일</th>
            <th class="text-right">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && posts.length === 0">
            <td colspan="8" class="empty">불러오는 중…</td>
          </tr>
          <tr v-else-if="posts.length === 0">
            <td colspan="8" class="empty">게시글이 없습니다.</td>
          </tr>
          <tr v-for="row in posts" :key="row.id">
            <td class="num">{{ row.id }}</td>
            <td class="body-cell">
              <div class="title">{{ row.title }}</div>
              <div class="excerpt">{{ row.excerpt }}</div>
            </td>
            <td>{{ row.authorNickname }}</td>
            <td class="work">{{ row.workTitle }}</td>
            <td class="num">{{ row.viewCount }}</td>
            <td class="num">{{ row.commentCount }}</td>
            <td class="date">{{ formatDate(row.createdAt) }}</td>
            <td class="text-right">
              <v-btn
                size="small"
                variant="outlined"
                color="error"
                :loading="deleting === row.id"
                :disabled="deleting !== null"
                @click="removePost(row)"
              >
                삭제
              </v-btn>
            </td>
          </tr>
        </tbody>
      </v-table>

      <!-- ── 댓글 ── -->
      <v-table v-else density="comfortable">
        <thead>
          <tr>
            <th class="num">ID</th>
            <th>내용</th>
            <th>작성자</th>
            <th>원글</th>
            <th>작성일</th>
            <th class="text-right">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && comments.length === 0">
            <td colspan="6" class="empty">불러오는 중…</td>
          </tr>
          <tr v-else-if="comments.length === 0">
            <td colspan="6" class="empty">댓글이 없습니다.</td>
          </tr>
          <tr v-for="row in comments" :key="row.id">
            <td class="num">{{ row.id }}</td>
            <td class="body-cell">
              <v-chip v-if="row.parentId" size="x-small" variant="tonal" class="mr-2">답글</v-chip>
              {{ row.content }}
            </td>
            <td>{{ row.authorNickname }}</td>
            <td class="work">{{ row.postTitle }}</td>
            <td class="date">{{ formatDate(row.createdAt) }}</td>
            <td class="text-right">
              <v-btn
                size="small"
                variant="outlined"
                color="error"
                :loading="deleting === row.id"
                :disabled="deleting !== null"
                @click="removeComment(row)"
              >
                삭제
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
      삭제는 되돌릴 수 없습니다. 글을 지우면 딸린 댓글과 추천도 함께 사라집니다.
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
  margin: 2px 0 18px;
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
.num {
  font-variant-numeric: tabular-nums;
  color: #94a3b8;
  width: 70px;
}
.body-cell {
  max-width: 380px;
}
.title {
  font-weight: 600;
  font-size: 14px;
  color: #0f1a2e;
}
.excerpt {
  font-size: 12.5px;
  color: #64748b;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}
.work {
  font-size: 13px;
  color: #475569;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.date {
  font-variant-numeric: tabular-nums;
  color: #64748b;
  font-size: 12.5px;
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
