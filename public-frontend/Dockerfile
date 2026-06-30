# syntax=docker/dockerfile:1.6
# =============================================================================
# Public site (Next.js App Router, SSR/SSG) — serves the reader-facing blog at /.
# =============================================================================

# ── Stage 1: build ──────────────────────────────────────────
FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# NEXT_PUBLIC_* is inlined at build time → must be known here.
ARG NEXT_PUBLIC_SITE_URL=https://news.luuhailong.com
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL
RUN npm run build

# ── Stage 2: runtime (standalone) ───────────────────────────
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3100
ENV HOSTNAME=0.0.0.0

RUN addgroup -g 1001 -S nodejs && adduser -u 1001 -G nodejs -S nextjs

# Standalone output bundles only what's needed to run `node server.js`.
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3100

# BLOG_API_URL (server→backend) is provided at runtime via the environment.
# Node-based check (busybox wget's --spider is unreliable in alpine).
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD node -e "require('http').get('http://localhost:3100/',r=>process.exit(r.statusCode<500?0:1)).on('error',()=>process.exit(1))"

CMD ["node", "server.js"]
