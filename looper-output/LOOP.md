# blog-p2-features

Implement 4 P2 features (RSS, Sitemap, Newsletter, Comments) for personal-blog-cms with TDD

## Goal

Implement 4 P2 features for the personal-blog-cms Spring Boot + React app: (1) RSS/Atom feed for blog posts, (2) XML sitemap for public routes, (3) Newsletter email send to subscribers, (4) Comment system with nested replies. Each feature must follow TDD (RED → GREEN → REFACTOR), end with git commit, and be browser-verified.

## Definition of Done

All 4 features shipped to main branch. Backend tests pass (target: 80+/80+). Frontend builds clean. Each feature has at least one curl/browser verify. README updated with new endpoints. No new bugs introduced.

## Verification

- `backend-build-ok` (programmatic)
- `backend-tests-pass` (programmatic)
- `frontend-build-ok` (programmatic)
- `rss-endpoint-valid` (programmatic)
- `sitemap-endpoint-valid` (programmatic)
- `newsletter-api-works` (programmatic)
- `comments-api-works` (programmatic)
- `code-quality` (judge)
- `seo-correctness` (judge)

## Council

- `reviewer-1`: judge via human (sếp-long)

## Gates

- Plan gate: revise_until_clean
- Delivery gate: revise_until_clean

## Loop Control

- Max iterations: 12
- Budget: `{"tokens": 2000000, "usd": 0.5, "wall_clock_min": 240}`
- No-progress: `{"action": "human_checkpoint", "max_stalled_iterations": 2, "signals": ["same feature blocked 2 iterations", "test failure pattern repeats", "s\u1ebfp rejected same code twice"]}`

## Execution Boundary

- Mode: `in_session`
- Isolation: `current_workspace`
- Side effects: `{"duplicate_action_check": true, "note": "Each feature requires s\u1ebfp approval before moving to next", "requires_approval": true}`

## Observability

- State file: `~/vibe-code/personal-blog-cms/looper-output/loop-workspace/state.json`
- Run log: `~/vibe-code/personal-blog-cms/looper-output/loop-workspace/run-log.md`
- Checkpoint granularity: `gate`

## Flow Preview

```text
+--------------------------------+
| 1. Goal + context              |
| read sources                   |
+--------------------------------+
               |
               v
+--------------------------------+
| 2. Draft plan.md               |
| state -> ~/vibe-code/personal~ |
+--------------------------------+
               |
               v
+--------------------------------+
| 3. Plan gate                   |
| verdict: reviewer-1            |
+--------------------------------+
               | needs work -> revise <= 2 -> step 2
               | pass
               v
+--------------------------------+
| 4. Write delivery-N.md         |
| log -> ~/vibe-code/personal-b~ |
+--------------------------------+
               |
               v
+--------------------------------+
| 5. Delivery gate               |
| verdict: reviewer-1            |
+--------------------------------+
               | needs work -> revise <= 2 -> step 4
               | pass
               v
+--------------------------------+
| 6. Final output                |
| all gates clean                |
+--------------------------------+

Stops: pass gates | max 12 iterations | no progress x2 | budget 240m, $0.5, 2000000 tokens
```
