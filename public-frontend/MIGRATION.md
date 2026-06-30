# Public site → Next.js (SSR/SSG) migration

**Why:** the public blog was a Vite React SPA (CSR). Non-JS crawlers (Facebook, X,
LinkedIn, Telegram, Zalo, many AI bots) received the homepage's generic meta for
*every* URL, and OG images were SVG (not rendered by social). Migrating to Next.js
App Router gives per-URL server-rendered meta + JSON-LD and dynamic PNG OG images.

**Scope:** public site only. Admin stays in `frontend/` (Vite SPA, served at `/admin`).
Backend (`/api/public/*`) is unchanged — Next fetches it server-side.

**Authoritative deploy:** `Dockerfile.nginx` + `deploy/nginx.conf`. nginx routes
`/` → Next.js (this app), `/admin` → Vite SPA, `/api` → Spring Boot.

## Progress

- [x] **1. Scaffold** — Next 15 App Router + TS + Tailwind + typography
- [x] **1. lib** — server API client (`src/lib/api.ts`), types, site config
- [x] **1. Root layout** — `<html lang="vi">`, site-wide metadata + JSON-LD Blog, header/footer
- [x] **1. `/blog/[slug]`** — full SSR `generateMetadata` (title/desc/canonical/OG/Twitter) + JSON-LD BlogPosting
- [x] **1. Dynamic OG image** — `/og` route via `next/og` → PNG 1200×630 (replaces SVG)
- [x] **1. sitemap.ts + robots.ts** — dynamic from API
- [x] **1. Home + blog list** — ported
- [x] **2. Remaining pages** — `/topics/[slug]`, `/projects`, `/projects/[slug]`, `/about`, `/now`, `/newsletter` (all SSR, build clean, verified titles/canonical)
- [x] **2. Per-page metadata** — generateMetadata + JSON-LD (BlogPosting / CollectionPage / SoftwareSourceCode / ProfilePage); newsletter form ported as client component
- [x] **3. Content fidelity** — ported parseCustomBlocks + CustomBlock (:::takeaways/callout/reference) into PostContent.tsx; /blog/[slug] now renders contentMarkdown (server-side) since backend doesn't reliably populate contentHtml. Build clean.
- [x] **3. OG font** — Be Vietnam Pro (Regular+SemiBold) bundled in public/fonts, read via fs in /og route. Verified: PNG 1200×630 with full Vietnamese diacritics (visually confirmed). Removed ✦ glyph (not in font → triggered failing network fallback).
- [x] **4. Tracking** — GA4/GTM/FB/TikTok via next/script (server-driven from settings) + GTM/FB noscript; custom head/css/body scripts via client injector (script-clone). Build clean. (TODO: re-add DOMPurify defense-in-depth + CSP header + consent-mode gating.)
- [x] **4. Newsletter form** — client component POST /api/public/newsletter/subscribe
- [x] **5. Deploy wiring** — `public-frontend/Dockerfile` (standalone), nginx `/`→public_web + `/admin`→Vite static + `/api`+`/uploads`+`/rss.xml`+`/covers`→backend; docker-compose `public-web` service, admin mount → frontend/dist, dropped empty public-frontend/dist mount. Validated: `docker compose config` OK + `nginx -t` OK.
- [x] **6. Verify** — Next standalone server run against the live backend (10 real posts). Curl of `/blog/<real-slug>` raw HTML confirmed: real per-post `<title>`, canonical, og:title/url/type=article, og:image=`/covers/…png` (real PNG cover → confirms `/covers/` URL format), twitter:card, JSON-LD BlogPosting, and Vietnamese body text — all server-rendered. sitemap.xml lists real post URLs. `docker compose build public-web` image builds OK. (NOT done: full `docker compose up` with nginx on :80 — would disturb the already-running backend/postgres and may conflict on :80; left as the operator's go-live step.)

## Full-stack e2e — verified through nginx ✅
`docker compose up -d` (nginx on :8088) brings up the whole new stack cleanly and
serves correctly against the live backend (10 real posts):
- `/` real posts · `/blog/<slug>` per-post title/canonical/og:url/**og:image**/twitter/JSON-LD BlogPosting + body — all on `https://news.luuhailong.com`
- `/sitemap.xml` 17 URLs incl. real posts · `/robots.txt` · `/rss.xml` (→backend) · `/covers/*.png` · `/og` PNG — all 200

### Bugs caught & fixed during e2e
- **Public domain leak**: compose passed `NEXT_PUBLIC_SITE_URL` from `APP_BASE_URL` (backend URL = localhost:8080 in dev) → canonical/og:url were localhost. Now a dedicated `PUBLIC_SITE_URL` (default the real domain).
- **Internal host in og:image**: backend bakes absolute cover URLs from its own base; `absUrl()` now rewrites internal hosts (localhost/blog-api) → SITE_URL.
- **Empty data at build**: home/blog/projects/sitemap were prerendered at build (no backend there) → `export const dynamic = 'force-dynamic'` so they SSR live.
- **Stack wouldn't `up`**: flaky wget healthchecks gated `depends_on: service_healthy`. public-web healthcheck → node-based; deps → `service_started`.

### Known follow-ups (not blocking)
- `blog-api` healthcheck still reports unhealthy (pre-existing: `wget`/`/actuator/health/liveness` in its image) — app works; fix the probe separately.
- about/now/newsletter stay statically prerendered → tracking from build-time settings only (fine while no tracking IDs set); make layout dynamic if needed.

## Status: migration complete ✅
All code items done; build clean; SEO goal verified against real data. Remaining are operator/ops steps, not code:
- `docker compose up -d --build` to run the full new stack (public-web + nginx) when ready.
- Admin SPA at `/admin` needs Vite `base:'/admin/'` + router basename (frontend/ still serves public routes too).
- Security follow-ups: DOMPurify defense-in-depth in CustomScripts, CSP header in nginx, consent-mode gating.
- Optional: expose post metaTitle/metaDescription/canonicalUrl in the public DTO to override derived meta.

### Deploy follow-ups (flagged, not yet done)
- Admin SPA at `/admin` needs Vite `base: '/admin/'` + router basename `/admin` (frontend/ currently serves public at `/` too — that part is now superseded by Next).
- Verify post `coverImageUrl` format (`/covers/…` vs `/uploads/…`) against a live post; `/covers/` mount assumes the former.
- `deploy/nginx.conf` + `Dockerfile.nginx` (single-image static path) are now superseded by the compose stack for the public site — consider removing or marking deprecated.

## Notes
- Post DTO has no metaTitle/metaDescription/ogImage/canonical — derive meta from
  title/excerpt/coverImageUrl. (Backend entity has them; could expose later.)
- Use `post.contentHtml` for SSR body (server-rendered by backend) where present.
- Env: `BLOG_API_URL` (server→backend, e.g. http://blog-api:8080), `NEXT_PUBLIC_SITE_URL`.
</content>
</invoke>
