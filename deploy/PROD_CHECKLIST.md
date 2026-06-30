# Production deploy checklist

The stack runs via `docker compose` (postgres + blog-api + public-web + nginx).
Go through this before exposing the site publicly.

## 1. Secrets & env (`.env`)
Copy `.env.example` → `.env` and set **real** values. `.env` is gitignored — never commit it.

- [ ] `DB_PASSWORD` — long random string (`openssl rand -base64 32`).
- [ ] `JWT_SECRET` — **must change** from the example (`openssl rand -base64 64`).
- [ ] `APP_BASE_URL=https://news.luuhailong.com` (backend's public base — media/reset links).
- [ ] `PUBLIC_SITE_URL=https://news.luuhailong.com` (Next canonical/OG/sitemap — baked at build).
- [ ] `COOKIE_SECURE=true` and `COOKIE_SAMESITE=Lax` (HTTPS only).
- [ ] `SPRING_PROFILES_ACTIVE=prod`.

> `PUBLIC_SITE_URL` is a **build arg** for the Next image — after changing it run
> `docker compose build public-web` (or `up -d --build`).

## 2. Admin account
The seed admin is `admin@example.com` (password set by migration V11). Before launch:

- [ ] **Change the admin email** to your own — either in the admin UI (Profile page)
      or SQL: `UPDATE users SET email='you@domain.com' WHERE email='admin@example.com';`
- [ ] **Change the admin password** in the admin UI (Profile → change password).
      (V11 sets a default; treat it as first-login-only.)

## 3. TLS / HTTPS
Two options:

**A. Cloudflare in front (recommended, simplest)** — Cloudflare terminates TLS;
nginx stays HTTP on :80. `COOKIE_SECURE=true` still applies (Cloudflare sets
`X-Forwarded-Proto: https`, which the backend trusts). Nothing else to do.

**B. TLS at nginx** —
- [ ] Put certs in `deploy/certs/`: `fullchain.pem` + `privkey.pem` (already mounted).
- [ ] Uncomment the `server { listen 443 ssl … }` block at the bottom of
      `deploy/nginx/conf.d/blog.conf` and copy the location blocks from the :80 server.
- [ ] Add an HTTP→HTTPS redirect on the :80 server.

## 4. Launch
```bash
docker compose up -d --build      # builds blog-api + public-web images
docker compose ps                 # all should be healthy
```
Smoke test (replace host): `/`, `/blog/<slug>`, `/sitemap.xml`, `/robots.txt`,
`/rss.xml`, `/admin/`.

## 5. SEO / post-launch
- [ ] Submit `https://news.luuhailong.com/sitemap.xml` in Google Search Console.
- [ ] Set tracking IDs (GA4/GTM/FB/TikTok) in admin → Settings → Tracking Scripts.
- [ ] Verify OG preview (Facebook Sharing Debugger / X Card Validator) on a post URL.

## 6. Hardening (see ROADMAP #9)
- [ ] CSP header in nginx, DOMPurify on custom scripts, cookie-consent gating.
- [ ] Restrict Postgres port exposure (compose binds 127.0.0.1 only — keep it).
