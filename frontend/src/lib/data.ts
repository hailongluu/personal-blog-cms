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
  preview: (id: number) =>
    api.get<{ data: Post }>(`/admin/posts/${id}/preview`).then(r => r.data.data),
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

// ─── Dashboard (SPEC §8.4) ─────────────────────────────────
export interface DashboardData {
  totalPosts: number;
  publishedPosts: number;
  draftPosts: number;
  reviewingPosts: number;
  archivedPosts: number;
  totalTopics: number;
  totalTags: number;
  totalProjects: number;
  totalMedia: number;
  newsletterSubscribers: number;
  recentPosts: Array<{
    id: number; title: string; slug: string; status: string;
    type: string; featured: boolean; updatedAt: string;
  }>;
  pendingDrafts: Array<{
    id: number; title: string; slug: string; status: string;
    type: string; updatedAt: string;
  }>;
}
export const dashboardApi = {
  get: () => api.get<{ data: DashboardData }>('/admin/dashboard').then(r => r.data.data),
};

// ─── Auth (admin) ─────────────────────────────────────────
export const authApi = {
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post<{ data: { message: string } }>('/admin/auth/change-password', {
      currentPassword,
      newPassword,
    }).then(r => r.data.data),

  forgotPassword: (email: string) =>
    api.post<{ data: { message: string } }>('/admin/auth/forgot-password', { email })
      .then(r => r.data.data),

  resetPassword: (token: string, newPassword: string) =>
    api.post<{ data: { message: string } }>('/admin/auth/reset-password', {
      token,
      newPassword,
    }).then(r => r.data.data),
};

// ─── Scheduled Tasks (admin) ─────────────────────────────
export interface CronJob {
  id: string;
  name: string;
  schedule: string;
  state: string;
  nextRun?: string | null;
  lastRun?: string | null;
  lastStatus?: string | null;
  deliver?: string;
  script?: string;
  mode?: string;
}

export interface ContentRegistryItem {
  id: number;
  slug: string;
  source: string;
  sourceUrl?: string;
  topic?: string;
  pillar?: string;
  funnel?: string;
  status: string;
  postId?: number | null;
  publishedAt?: string;
  createdAt?: string;
}

export interface ContentRegistryPage {
  items: ContentRegistryItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface ScheduledPostItem {
  id: number;
  title: string;
  slug: string;
  status: string;
  scheduledAt?: string | null;
  timeUntilPublish?: string;
  authorId?: number | null;
}

export interface NewsletterLogItem {
  id: number;
  subject: string;
  recipientCount: number;
  successCount: number;
  failureCount: number;
  sentBy?: number | null;
  sentAt: string;
  deliveryStatus: string;
}

export interface ScheduledTasksAggregate {
  cronJobs: CronJob[];
  cronJobsError?: string | null;
  contentRegistryCollectedCount: number;
  contentRegistryPublishedCount: number;
  scheduledPosts: ScheduledPostItem[];
  newsletterLog: NewsletterLogItem[];
}

export const scheduledTasksApi = {
  // Aggregate view (one call returns all 4 sections)
  aggregate: () =>
    api.get<{ data: ScheduledTasksAggregate }>('/admin/scheduled-tasks').then(r => r.data.data),

  // 1. Cron jobs
  listCron: () =>
    api.get<{ data: CronJob[] }>('/admin/scheduled-tasks/cron').then(r => r.data.data),
  runCron: (jobId: string) =>
    api.post<{ data: string }>(`/admin/scheduled-tasks/cron/${jobId}/run`).then(r => r.data.data),
  pauseCron: (jobId: string) =>
    api.post<{ data: string }>(`/admin/scheduled-tasks/cron/${jobId}/pause`).then(r => r.data.data),
  resumeCron: (jobId: string) =>
    api.post<{ data: string }>(`/admin/scheduled-tasks/cron/${jobId}/resume`).then(r => r.data.data),
  deleteCron: (jobId: string) =>
    api.delete<{ data: string }>(`/admin/scheduled-tasks/cron/${jobId}`).then(r => r.data.data),

  // 2. Content registry
  listContentRegistry: (params: {
    page?: number; size?: number; source?: string; pillar?: string; funnel?: string; status?: string;
  } = {}) => {
    const q: Record<string, string | number> = {};
    if (params.page !== undefined) q.page = params.page;
    if (params.size !== undefined) q.size = params.size;
    if (params.source) q.source = params.source;
    if (params.pillar) q.pillar = params.pillar;
    if (params.funnel) q.funnel = params.funnel;
    if (params.status) q.status = params.status;
    return api.get<{ data: ContentRegistryPage }>('/admin/scheduled-tasks/content-registry', { params: q })
      .then(r => r.data.data);
  },
  rejectContentRegistry: (id: number) =>
    api.post<{ data: null }>(`/admin/scheduled-tasks/content-registry/${id}/reject`).then(r => r.data.data),
  deleteContentRegistry: (id: number) =>
    api.delete<{ data: null }>(`/admin/scheduled-tasks/content-registry/${id}`).then(r => r.data.data),

  // 3. Scheduled posts
  listScheduledPosts: () =>
    api.get<{ data: ScheduledPostItem[] }>('/admin/scheduled-tasks/posts/scheduled').then(r => r.data.data),
  publishPostNow: (id: number) =>
    api.post<{ data: { id: number; status: string } }>(`/admin/scheduled-tasks/posts/${id}/publish-now`)
      .then(r => r.data.data),
  reschedulePost: (id: number, scheduledAt: string) =>
    api.post<{ data: ScheduledPostItem }>(`/admin/scheduled-tasks/posts/${id}/reschedule`, { scheduledAt })
      .then(r => r.data.data),
  cancelScheduledPost: (id: number) =>
    api.post<{ data: ScheduledPostItem }>(`/admin/scheduled-tasks/posts/${id}/cancel-schedule`)
      .then(r => r.data.data),

  // 4. Newsletter log
  listNewsletterLog: () =>
    api.get<{ data: NewsletterLogItem[] }>('/admin/scheduled-tasks/newsletter').then(r => r.data.data),
  resendNewsletter: (id: number) =>
    api.post<{ data: null }>(`/admin/scheduled-tasks/newsletter/${id}/resend`).then(r => r.data.data),
  deleteNewsletterLog: (id: number) =>
    api.delete<{ data: null }>(`/admin/scheduled-tasks/newsletter/${id}`).then(r => r.data.data),
};
