// 커뮤니티 API — 작품별 게시판
//
// 읽기(GET)는 비로그인도 가능하고, 쓰기는 인증이 필요하다.
// apiFetch 가 토큰을 자동으로 붙이므로 로그인 상태면 '내 글인지'까지 함께 내려온다.

import { apiFetch } from "../../lib/api";
import type { PageResponse } from "../work/types";

/** 목록 한 줄 — 백엔드 PostSummaryResponse */
/**
 * 채널(=작품) 카드 한 장. 백엔드 ChannelResponse.
 *
 * ★latestPost* 가 null 일 수 있다★
 * 커뮤니티 첫 화면(/api/community/channels)은 글이 있는 채널만 주므로 항상 채워져 오지만,
 * 내 구독 목록(/api/subscriptions)은 "내가 구독한 것"이 기준이라
 * 글이 아직 하나도 없는 채널도 들어 있다.
 */
export type Channel = {
  workId: number;
  workTitle: string;
  workImageUrl: string | null;
  postCount: number;
  latestPostId: number | null;
  latestPostTitle: string | null;
  latestPostAuthor: string | null;
  latestPostCreatedAt: string | null;
};

export type PostSummary = {
  id: number;
  title: string;
  authorNickname: string;
  authorId: number;
  viewCount: number;
  commentCount: number;
  likeCount: number;
  createdAt: string;
};

/** 상세 — 백엔드 PostDetailResponse */
export type PostDetail = {
  id: number;
  workId: number;
  workTitle: string;
  title: string;
  content: string;
  authorNickname: string;
  authorId: number;
  viewCount: number;
  commentCount: number;
  likeCount: number;
  /** 지금 보는 사람이 추천했는지 (비로그인이면 false) */
  likedByMe: boolean;
  /** 지금 보는 사람이 작성자인지 — 수정·삭제 버튼 노출 판단 */
  mine: boolean;
  createdAt: string;
  updatedAt: string;
};

export type Comment = {
  id: number;
  content: string;
  authorNickname: string;
  authorId: number;
  mine: boolean;
  createdAt: string;
};

/* ── 글 ─────────────────────────────────────────────── */

/** 작품 게시판 목록 (최신순) */
export function fetchPosts(workId: number, page = 0, size = 20): Promise<PageResponse<PostSummary>> {
  return apiFetch<PageResponse<PostSummary>>(`/api/works/${workId}/posts?page=${page}&size=${size}`);
}

/** 글 상세 — 부를 때마다 조회수가 오른다 */
export function fetchPost(id: number): Promise<PostDetail> {
  return apiFetch<PostDetail>(`/api/posts/${id}`);
}

/**
 * 글 작성.
 *
 * <p>백엔드는 201 + Location 헤더로 응답한다(바디 없음).
 * apiFetch 가 빈 바디를 undefined 로 돌려주므로 반환값을 쓰지 않는다.
 */
export function createPost(workId: number, title: string, content: string): Promise<void> {
  return apiFetch<void>(`/api/works/${workId}/posts`, {
    method: "POST",
    body: JSON.stringify({ title, content }),
  });
}

export function updatePost(id: number, title: string, content: string): Promise<void> {
  return apiFetch<void>(`/api/posts/${id}`, {
    method: "PUT",
    body: JSON.stringify({ title, content }),
  });
}

/** 글 삭제 — 댓글·추천도 함께 지워진다 */
export function deletePost(id: number): Promise<void> {
  return apiFetch<void>(`/api/posts/${id}`, { method: "DELETE" });
}

/* ── 댓글 ───────────────────────────────────────────── */

export function fetchComments(postId: number): Promise<Comment[]> {
  return apiFetch<Comment[]>(`/api/posts/${postId}/comments`);
}

export function createComment(postId: number, content: string): Promise<void> {
  return apiFetch<void>(`/api/posts/${postId}/comments`, {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}

export function deleteComment(commentId: number): Promise<void> {
  return apiFetch<void>(`/api/comments/${commentId}`, { method: "DELETE" });
}

/* ── 추천 ───────────────────────────────────────────── */

/** 추천 — 찜과 같은 이유로 PUT (몇 번을 보내도 결과가 같다) */
export function likePost(id: number): Promise<void> {
  return apiFetch<void>(`/api/posts/${id}/like`, { method: "PUT" });
}

export function unlikePost(id: number): Promise<void> {
  return apiFetch<void>(`/api/posts/${id}/like`, { method: "DELETE" });
}

/** 목록·상세에 쓰는 날짜 표기 — "8월 26일 14:30" */
export function formatPostDate(iso: string): string {
  const d = new Date(iso);
  const now = new Date();
  const sameDay =
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate();

  const time = `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  // 오늘 쓴 글은 시각만 — 게시판에서 새 글을 알아보기 쉽게
  return sameDay ? time : `${d.getMonth() + 1}월 ${d.getDate()}일 ${time}`;
}
