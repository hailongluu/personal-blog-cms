export type PostType =
  | 'essay' | 'research_brief' | 'field_note' | 'build_log'
  | 'playbook' | 'review' | 'personal_log';

export const POST_TYPES: { value: PostType; label: string }[] = [
  { value: 'essay', label: 'Essay' },
  { value: 'research_brief', label: 'Research Brief' },
  { value: 'field_note', label: 'Field Note' },
  { value: 'build_log', label: 'Build Log' },
  { value: 'playbook', label: 'Playbook' },
  { value: 'review', label: 'Review' },
  { value: 'personal_log', label: 'Personal Log' },
];

export interface Post {
  id: number;
  title: string;
  slug: string;
  subtitle: string | null;
  excerpt: string | null;
  contentMarkdown: string;
  contentHtml: string;
  coverImageUrl: string | null;
  status: 'draft' | 'reviewing' | 'published' | 'archived';
  visibility: 'public' | 'unlisted' | 'private';
  type: PostType;
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

export interface Tag {
  id: number;
  name: string;
  slug: string;
  createdAt: string;
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

export interface Media {
  id: number;
  filename: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  publicUrl: string;
  altText: string | null;
  caption: string | null;
  createdAt: string;
}

export interface PagedResponse<T> {
  data: T[];
  meta: { page: number; pageSize: number; totalItems: number; totalPages: number };
}

export interface Settings {
  "site.title"?: string;
  "site.description"?: string;
  "site.author"?: string;
  "site.author_bio"?: string;
  "social.github"?: string;
  "social.linkedin"?: string;
  "social.x"?: string;
  "social.youtube"?: string;
  "site.email"?: string;
  // ── Tracking scripts ─────────────────────────────────────
  "tracking.ga4_measurement_id"?: string;
  "tracking.gtm_container_id"?: string;
  "tracking.fb_pixel_id"?: string;
  "tracking.tiktok_pixel_id"?: string;
  "tracking.gtag_enabled"?: boolean;
  "tracking.fb_enabled"?: boolean;
  "tracking.tiktok_enabled"?: boolean;
  "tracking.consent_mode"?: "none" | "basic" | "full" | string;
  // ── Custom scripts/CSS ───────────────────────────────────
  "custom.head_scripts"?: string;
  "custom.body_start_scripts"?: string;
  "custom.body_end_scripts"?: string;
  "custom.css"?: string;
}

export interface ApiResponse<T> {
  data: T;
  error?: { code: string; message: string };
  meta?: { page: number; pageSize: number; totalItems: number; totalPages: number };
}
