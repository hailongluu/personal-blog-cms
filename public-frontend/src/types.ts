// Mirrors the public DTOs returned by the Spring Boot backend (/api/public/*).
// Kept in sync with frontend/src/types/index.ts (public subset).

export interface Post {
  id: number;
  title: string;
  slug: string;
  subtitle: string | null;
  excerpt: string | null;
  contentMarkdown: string;
  contentHtml: string;
  coverImageUrl: string | null;
  type: string;
  featured: boolean;
  author: { id: number; displayName: string; avatarUrl: string | null };
  topic: { id: number; name: string; slug: string; color: string } | null;
  tags: { id: number; name: string; slug: string }[];
  readingTimeMin: number;
  viewCount: number;
  publishedAt: string | null;
  firstPublishedAt: string | null;
  lastPublishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Topic {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  color: string | null;
  icon: string | null;
  parentId: number | null;
  children: Topic[];
  sortOrder: number;
}

export interface Project {
  id: number;
  title: string;
  slug: string;
  description: string | null;
  contentMarkdown: string | null;
  coverImageUrl: string | null;
  projectUrl: string | null;
  repoUrl: string | null;
  techStack: string[];
  status: string;
  isFeatured: boolean;
  sortOrder: number;
}

export interface PageMeta {
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
}

export interface PagedResponse<T> {
  data: T[];
  meta: PageMeta;
}

export interface ApiResponse<T> {
  data: T;
  error?: { code: string; message: string };
  meta?: PageMeta;
}

export interface Comment {
  id: number;
  postId: number;
  parentId: number | null;
  authorName: string;
  status: string;
  content: string;
  createdAt: string;
  moderatedAt: string | null;
}

export type Settings = Record<string, string | boolean | undefined>;
