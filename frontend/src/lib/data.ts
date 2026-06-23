import { api } from '@/lib/api';
import type { Post, Topic, Tag, Project, Media, Settings, PagedResponse } from '@/types';
// ─── Posts ────────────────────────────────────────────────
export const postsApi = {
  list: (params?: Record<string, string | number | boolean>) =>
    api.get<PagedResponse<Post>>('/admin/posts', { params }).then(r => r.data),
  get: (id: number) => api.get<{ data: Post }>(`/admin/posts/${id}`).then(r => r.data.data),
  getBySlug: (slug: string) => api.get<{ data: Post }>(`/admin/posts/slug/${slug}`).then(r => r.data.data),
  create: (data: Partial<Post> & { topicId?: number; tagIds?: number[] }) =>
    api.post<{ data: Post }>('/admin/posts', data).then(r => r.data.data),
  update: (id: number, data: Partial<Post> & { topicId?: number; tagIds?: number[] }) =>
    api.put<{ data: Post }>(`/admin/posts/${id}`, data).then(r => r.data.data),
  delete: (id: number) => api.delete(`/admin/posts/${id}`),
  restore: (id: number) => api.post(`/admin/posts/${id}/restore`),
  publish: (id: number) =>
    api.post<{ data: Post }>(`/admin/posts/${id}/publish`).then(r => r.data.data),
  unpublish: (id: number) =>
    api.post<{ data: Post }>(`/admin/posts/${id}/unpublish`).then(r => r.data.data),
  archive: (id: number) =>
    api.post<{ data: Post }>(`/admin/posts/${id}/archive`).then(r => r.data.data),
  duplicate: (id: number) =>
    api.post<{ data: Post }>(`/admin/posts/${id}/duplicate`).then(r => r.data.data),
};

// ─── Topics ───────────────────────────────────────────────
export const topicsApi = {
  list: () => api.get<{ data: Topic[] }>('/admin/topics').then(r => r.data.data),
  get: (id: number) => api.get<{ data: Topic }>(`/admin/topics/${id}`).then(r => r.data.data),
  create: (data: Partial<Topic>) => api.post<{ data: Topic }>('/admin/topics', data).then(r => r.data.data),
  update: (id: number, data: Partial<Topic>) => api.put<{ data: Topic }>(`/admin/topics/${id}`, data).then(r => r.data.data),
  delete: (id: number) => api.delete(`/admin/topics/${id}`),
};

// ─── Tags ─────────────────────────────────────────────────
export const tagsApi = {
  list: () => api.get<{ data: Tag[] }>('/admin/tags').then(r => r.data.data),
  create: (data: { name: string; slug?: string }) => api.post<{ data: Tag }>('/admin/tags', data).then(r => r.data.data),
  delete: (id: number) => api.delete(`/admin/tags/${id}`),
};

// ─── Projects ─────────────────────────────────────────────
export const projectsApi = {
  list: () => api.get<PagedResponse<Project>>('/admin/projects').then(r => r.data),
  get: (id: number) => api.get<{ data: Project }>(`/admin/projects/${id}`).then(r => r.data.data),
  create: (data: Partial<Project>) => api.post<{ data: Project }>('/admin/projects', data).then(r => r.data.data),
  update: (id: number, data: Partial<Project>) => api.put<{ data: Project }>(`/admin/projects/${id}`, data).then(r => r.data.data),
  delete: (id: number) => api.delete(`/admin/projects/${id}`),
};

// ─── Media ────────────────────────────────────────────────
export const mediaApi = {
  list: () => api.get<PagedResponse<Media>>('/admin/media').then(r => r.data),
  upload: (file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return api.post<{ data: Media }>('/admin/media/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data.data);
  },
  delete: (id: number) => api.delete(`/admin/media/${id}`),
};

// ─── Settings ──────────────────────────────────────────────
export const settingsApi = {
  getAdmin: () => api.get<{ data: Settings }>('/admin/settings').then(r => r.data.data),
  update: (data: Settings) => api.patch<{ data: Settings }>('/admin/settings', data).then(r => r.data.data),
  getPublic: () => api.get<{ data: Settings }>('/public/settings').then(r => r.data.data),
};
