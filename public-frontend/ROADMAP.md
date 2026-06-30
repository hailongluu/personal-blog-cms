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
- [x] **7. Commit gọn** — branch `feat/blog-enhancements`, 6 commit logic (backend/migrations/public-frontend/admin/deploy/assets).
  Ignored wordpress/ (separate project w/ own secret), .claude/, .mcp.json, .codegraph/. No secrets in history. Not pushed.
- [x] **8. Prod config** — .env.example updated (PUBLIC_SITE_URL decoupled), deploy/PROD_CHECKLIST.md (secrets/JWT,
  admin email+password, TLS via Cloudflare or nginx 443, launch, SEO, hardening). nginx 443 block ready (commented). Committed.
- [x] **9. Hardening** — nginx CSP + security headers on public location (verified via curl -I, SSR intact);
  DOMPurify (isomorphic-dompurify) defense-in-depth in CustomScripts; consent_mode='none' tracking kill-switch. Committed.
- [x] **10. Per-post meta** — added metaTitle/metaDescription/canonicalUrl/ogImageUrl to Next Post type;
  generateMetadata + JSON-LD prefer them, fall back to title/excerpt/cover/dynamic OG. Verified fallback intact. Committed.
- [x] **11. UX đọc** — ReadingProgress bar, PostToc (TOC từ markdown h2/h3, ids khớp rehype-slug), dark mode
  (class strategy + ThemeToggle + no-FOUC script + dark variants). Verified heading ids/TOC/toggle trong HTML.
  next/image **deferred**: backend trả cover URL host nội bộ → optimizer trong container không fetch; cần same-origin covers / custom loader.
- [x] **12. Newsletter** — double opt-in: subscribe issues confirm token (pending) + sends confirm email; GET /api/public/newsletter/confirm?token + /unsubscribe?email; Next pages /newsletter/confirm + /unsubscribe. Verified full flow pending→confirmed→unsubscribed.
- [x] **13. AI/hermes** — deploy/AI_HERMES_INTEGRATION.md (how ScheduledTasksService calls host hermes CLI, enable steps, AI hooks); ShareButtons (X/FB/Telegram/copy) on posts. hermes binary not bundled (host tool).

## Lưu ý chung
- Backend items (2,10,12,13) cần rebuild image `blog-api` (Maven) + recreate.
- Có 1 comment test ("Độc giả A") trên post id=11 — xoá trong admin moderation nếu không muốn giữ.
</content>
