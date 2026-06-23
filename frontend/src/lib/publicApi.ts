import axios from 'axios';
import type { Post, Topic, Project, PagedResponse, ApiResponse } from '@/types';

const publicApi = axios.create({
  baseURL: '/api/public',
  headers: { 'Content-Type': 'application/json' },
});

// ─── Posts ────────────────────────────────────────────────
export async function getPublishedPosts(page = 1, pageSize = 10, topicSlug?: string) {
  const params: Record<string, string | number> = { page, pageSize };
  if (topicSlug) params.topic = topicSlug;
  const res = await publicApi.get<PagedResponse<Post>>('/posts', { params });
  return res.data;
}

export async function getPostBySlug(slug: string) {
  const res = await publicApi.get<ApiResponse<Post>>(`/posts/${slug}`);
  return res.data.data;
}

export async function getFeaturedPosts(limit = 3) {
  const res = await publicApi.get<PagedResponse<Post>>('/posts/featured', {
    params: { limit },
  });
  return res.data.data;
}

// ─── Topics ───────────────────────────────────────────────
export async function getTopics() {
  const res = await publicApi.get<ApiResponse<Topic[]>>('/topics');
  return res.data.data;
}

export async function getTopicBySlug(slug: string) {
  const res = await publicApi.get<ApiResponse<Topic>>(`/topics/${slug}`);
  return res.data.data;
}

export async function getTopicPosts(slug: string, page = 1, pageSize = 10) {
  const res = await publicApi.get<PagedResponse<Post>>(`/topics/${slug}/posts`, {
    params: { page, pageSize },
  });
  return res.data;
}

// ─── Projects ─────────────────────────────────────────────
export async function getProjects() {
  const res = await publicApi.get<PagedResponse<Project>>('/projects');
  return res.data;
}

export async function getProjectBySlug(slug: string) {
  const res = await publicApi.get<ApiResponse<Project>>(`/projects/${slug}`);
  return res.data.data;
}

// ─── Newsletter ───────────────────────────────────────────
export async function subscribeNewsletter(email: string) {
  const res = await publicApi.post<ApiResponse<{ message: string }>>('/newsletter/subscribe', { email });
  return res.data;
}
