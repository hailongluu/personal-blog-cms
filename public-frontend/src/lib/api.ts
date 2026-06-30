// Server-side data access for the public site. Runs in Node (RSC / route handlers),
// fetching the Spring Boot backend's public API. Never import from a Client Component.

import type {
  ApiResponse,
  Comment,
  PagedResponse,
  Post,
  Project,
  Settings,
  Topic,
} from '@/types';

const API_BASE = (process.env.BLOG_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');

const DEFAULT_REVALIDATE = 60; // seconds (ISR)

async function apiGet<T>(path: string, revalidate = DEFAULT_REVALIDATE): Promise<T | null> {
  const url = `${API_BASE}/api/public${path}`;
  try {
    const res = await fetch(url, {
      headers: { Accept: 'application/json' },
      next: { revalidate },
    });
    if (res.status === 404) return null;
    if (!res.ok) {
      console.error(`[api] ${res.status} ${url}`);
      return null;
    }
    return (await res.json()) as T;
  } catch (err) {
    console.error(`[api] fetch failed: ${url}`, err);
    return null;
  }
}

// ─── Settings ─────────────────────────────────────────────
export async function getPublicSettings(): Promise<Settings> {
  const res = await apiGet<ApiResponse<Settings>>('/settings');
  return res?.data ?? {};
}

// ─── Posts ────────────────────────────────────────────────
export async function getPublishedPosts(
  page = 1,
  pageSize = 10,
  topicSlug?: string,
): Promise<PagedResponse<Post>> {
  const qs = new URLSearchParams({ page: String(page), size: String(pageSize) });
  if (topicSlug) qs.set('topic', topicSlug);
  const res = await apiGet<PagedResponse<Post>>(`/posts?${qs.toString()}`);
  return res ?? { data: [], meta: { page, pageSize, totalItems: 0, totalPages: 0 } };
}

export async function getPostBySlug(slug: string): Promise<Post | null> {
  const res = await apiGet<ApiResponse<Post>>(`/posts/${encodeURIComponent(slug)}`);
  return res?.data ?? null;
}

export async function getFeaturedPosts(limit = 3): Promise<Post[]> {
  const res = await apiGet<PagedResponse<Post>>(`/posts/featured?limit=${limit}`);
  return res?.data ?? [];
}

/** All published post slugs, for sitemap / static params. */
export async function getAllPostSlugs(): Promise<{ slug: string; updatedAt: string | null }[]> {
  const out: { slug: string; updatedAt: string | null }[] = [];
  let page = 1;
  // Page through up to a sane cap so the sitemap stays bounded.
  for (let i = 0; i < 50; i++) {
    const res = await getPublishedPosts(page, 100);
    out.push(...res.data.map((p) => ({ slug: p.slug, updatedAt: p.updatedAt })));
    if (page >= (res.meta.totalPages || 1)) break;
    page++;
  }
  return out;
}

// ─── Topics ───────────────────────────────────────────────
export async function getTopics(): Promise<Topic[]> {
  const res = await apiGet<ApiResponse<Topic[]>>('/topics');
  return res?.data ?? [];
}

export async function getTopicBySlug(slug: string): Promise<Topic | null> {
  const res = await apiGet<ApiResponse<Topic>>(`/topics/${encodeURIComponent(slug)}`);
  return res?.data ?? null;
}

export async function getTopicPosts(slug: string, page = 1, pageSize = 10): Promise<PagedResponse<Post>> {
  const qs = new URLSearchParams({ page: String(page), size: String(pageSize) });
  const res = await apiGet<PagedResponse<Post>>(`/topics/${encodeURIComponent(slug)}/posts?${qs}`);
  return res ?? { data: [], meta: { page, pageSize, totalItems: 0, totalPages: 0 } };
}

// ─── Projects ─────────────────────────────────────────────
export async function getProjects(): Promise<Project[]> {
  const res = await apiGet<PagedResponse<Project>>('/projects');
  return res?.data ?? [];
}

export async function getProjectBySlug(slug: string): Promise<Project | null> {
  const res = await apiGet<ApiResponse<Project>>(`/projects/${encodeURIComponent(slug)}`);
  return res?.data ?? null;
}

export async function getPostsByTag(slug: string, page = 1, pageSize = 10): Promise<PagedResponse<Post>> {
  const qs = new URLSearchParams({ page: String(page), size: String(pageSize) });
  const res = await apiGet<PagedResponse<Post>>(`/tags/${encodeURIComponent(slug)}/posts?${qs}`, 60);
  return res ?? { data: [], meta: { page, pageSize, totalItems: 0, totalPages: 0 } };
}

export async function getRelatedPosts(slug: string, limit = 3): Promise<Post[]> {
  const res = await apiGet<ApiResponse<Post[]>>(`/posts/${encodeURIComponent(slug)}/related?limit=${limit}`, 60);
  return res?.data ?? [];
}

// ─── Search ───────────────────────────────────────────────
export async function searchPosts(q: string, page = 1, pageSize = 10): Promise<PagedResponse<Post>> {
  const query = q.trim();
  if (!query) return { data: [], meta: { page, pageSize, totalItems: 0, totalPages: 0 } };
  const qs = new URLSearchParams({ q: query, page: String(page), size: String(pageSize) });
  const res = await apiGet<PagedResponse<Post>>(`/search?${qs.toString()}`, 30);
  return res ?? { data: [], meta: { page, pageSize, totalItems: 0, totalPages: 0 } };
}

// ─── Newsletter (double opt-in) ───────────────────────────
export async function confirmNewsletter(token: string): Promise<ApiResponse<null> | null> {
  return apiGet<ApiResponse<null>>(`/newsletter/confirm?token=${encodeURIComponent(token)}`, 0);
}
export async function unsubscribeNewsletter(email: string): Promise<ApiResponse<null> | null> {
  return apiGet<ApiResponse<null>>(`/newsletter/unsubscribe?email=${encodeURIComponent(email)}`, 0);
}

// ─── Comments ─────────────────────────────────────────────
export async function getComments(postId: number): Promise<Comment[]> {
  const res = await apiGet<ApiResponse<Comment[]>>(`/comments/post/${postId}`, 30);
  return res?.data ?? [];
}
