# Post-migration roadmap — 13 hạng mục hoàn thiện

Verify mỗi item: build sạch + (nếu đụng backend) rebuild image + test thật qua nginx :8088.
Stack chạy local: `NGINX_HTTP_PORT=8088 docker compose up -d`. Admin: admin@example.com / Admin@1110.

- [x] **1. Comments** — UI Next (Comments.tsx SSR + CommentForm client), gate theo `posts.allow_comments`
  (đã thêm key vào SettingsService.ALLOWED_KEYS → toggle từ CMS). **Fix bug backend**: `Comment.ipAddress`
  thiếu `@JdbcTypeCode(SqlTypes.INET)` → insert lỗi 42804. Verified: submit→pending→approve→render server-side.
- [x] **2. Search** — `GET /api/public/search?q=&page=&size=` (PublicController→PublicPostService.searchPublished→searchFullText),
  Next `/search` page (noindex,follow), `SearchBox` ở header (desktop+mobile), lib `searchPosts()`.
  Fix bug pre-existing: api.ts gửi `pageSize` nhưng backend đọc `size` → blog list luôn lấy 10. Verified q=AI→9 kết quả qua nginx.
- [x] **3. Related posts** — `GET /api/public/posts/{slug}/related?limit=` (same topic → fallback recent, exclude self),
  `RelatedPosts.tsx` (server) + `getRelatedPosts()`, render cuối /blog/[slug]. Verified qua nginx.
- [x] **4. Pagination / topics index / tag pages** — Pagination.tsx; `/blog?page=` (size=12); `/topics` index;
  backend `GET /api/public/tags/{slug}/posts` (PublicPostService.getPostsByTagSlug) + `/tags/[slug]` + tag chips→links.
  Verified build + nginx. (DB hiện chưa có tag nào: post_tags=0 → tag pages sẵn sàng nhưng chưa có data.)
- [x] **5. Admin base path** — frontend/ Vite `base:'/admin/'` + router `basename="/admin"`, admin routes → root,
  removed public routes (Next serves them), fixed all internal /admin links. Fixed nginx bug: `/admin/assets` regex
  location used `alias` (gotcha→404) → `root`. Verified: /admin/ 200, assets 200, deep links 200, public / still Next.
- [x] **6. blog-api healthcheck** — probe `/actuator/health/liveness` (401, Security-blocked) → `/actuator/health` (200 UP);
  reverted depends_on → service_healthy. `docker compose up -d` self-starts clean (no --no-deps), all healthy. Verified smoke test.
- [ ] **7. Commit gọn** — toàn bộ đang uncommitted; chia commit có ý nghĩa.
- [ ] **8. Prod config** — đổi admin email, secrets/env thật, bật SSL (block 443 nginx + cert).
- [ ] **9. Hardening** — CSP header, DOMPurify cho CustomScripts, cookie consent (consent_mode gating).
- [ ] **10. Per-post meta** — expose metaTitle/metaDescription/canonicalUrl ra public DTO + dùng trong generateMetadata.
- [ ] **11. UX đọc** — TOC, reading progress, dark mode, next/image cho cover/uploads.
- [ ] **12. Newsletter** — double opt-in confirm + trang unsubscribe + digest tự động.
- [ ] **13. AI/hermes** — auto-summary, gợi ý tag, tóm tắt (tận dụng ScheduledTasks/hermes).

## Lưu ý chung
- Backend items (2,10,12,13) cần rebuild image `blog-api` (Maven) + recreate.
- Có 1 comment test ("Độc giả A") trên post id=11 — xoá trong admin moderation nếu không muốn giữ.
</content>
