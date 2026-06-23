# Personal Blog CMS

A premium personal intelligence hub + admin CMS, designed for a single author who wants editorial-grade control over their content.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Browser (Reader)                      │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS
                         ▼
┌──────────────────────────────────────────────────────────┐
│                       Nginx                              │
│  /admin/*   → Admin SPA (React/Vite)                     │
│  /          → Public SPA (React/Vite or Next.js)         │
│  /api/*     → Spring Boot REST API                       │
│  /uploads/* → Local media storage                        │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│              Spring Boot (Java 21 LTS)                   │
│  ┌────────────┬────────────┬────────────┬────────────┐   │
│  │ Auth + JWT │ Posts CRUD │ Media      │ Newsletter │   │
│  └────────────┴────────────┴────────────┴────────────┘   │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│              PostgreSQL 17 (Flyway migrations)           │
└──────────────────────────────────────────────────────────┘
```

## Stack

| Layer | Technology |
|-------|------------|
| Admin Frontend | React 18 + Vite + TypeScript + Tailwind + shadcn/ui |
| Public Frontend | React/Vite (MVP) or Next.js SSG (v2) |
| Backend | Java 21 + Spring Boot 3 + Spring Security + Spring Data JPA |
| Database | PostgreSQL 17 with Flyway migrations |
| Web Server | Nginx (reverse proxy, static, SSL) |
| Deployment | Docker Compose on a single VPS (4 core / 8 GB RAM) |

## Quick start (development)

### Prerequisites
- Java 21 LTS
- Maven 3.8+
- Node.js 20+ LTS
- Docker + Docker Compose
- 2 GB free RAM for local containers

### Run locally

```bash
# 1. Copy env template
cp .env.example .env
# Edit .env to set DB_PASSWORD and JWT_SECRET

# 2. Start Postgres + Spring Boot API
docker compose up -d postgres blog-api

# 3. Run migrations + verify
curl http://localhost:8080/actuator/health

# 4. Start admin frontend (separate terminal)
cd admin-frontend
npm install
npm run dev
# → http://localhost:5173

# 5. Start public frontend (separate terminal)
cd public-frontend
npm install
npm run dev
# → http://localhost:5174
```

### Production deployment

See `docs/DEPLOY.md` (TODO) for the full production checklist.

## Repository structure

```
personal-blog-cms/
├── backend/                  Spring Boot API
│   ├── src/main/java/        Java source
│   ├── src/main/resources/   application.yml + Flyway migrations
│   └── Dockerfile
│
├── admin-frontend/           Admin CMS UI (React/Vite)
│   ├── src/
│   └── package.json
│
├── public-frontend/          Public blog UI
│   ├── src/
│   └── package.json
│
├── deploy/                   Production configs
│   ├── nginx/                nginx.conf
│   ├── postgres/             postgresql.conf
│   └── scripts/              backup-db.sh, restore-db.sh
│
├── docker-compose.yml
├── .env.example
└── README.md
```

## Content lifecycle

```
DRAFT → REVIEWING → PUBLISHED → ARCHIVED
   ↓          ↓           ↓
DELETED  DELETED    DELETED (soft)
```

All deletes are soft (`deleted_at` timestamp). Hard delete requires admin super-action.

## Documentation

- `SPEC.md` — Full system specification (1100+ lines)
- `docs/HARNESS.md` — Harness workflow
- `docs/loops/` — Loop engineering specs (core + domain)
- `backend/src/main/resources/db/migration/` — Flyway migration files

## License

Private project. All rights reserved.
