# Run `blog-p2-features` In This Session

Use this prompt when the user wants to run the Looper-designed loop in the current LLM session.
This is the default/easy execution path. The Python runner is the advanced path for running later or outside the session.

## Operator Instructions

You are executing a Looper-designed loop in this current session.
Follow the resolved spec below, write handoff files into the workspace, and enforce the caps manually.
Do not use `run-loop.py` unless the user explicitly asks for the advanced external runner.

1. Create the workspace directory if it does not exist.
2. Read the context sources before drafting the plan.
3. Draft `plan.md` in the workspace.
4. Run the plan gate. Apply programmatic checks when available. For judge criteria, use the configured judge only after consent for any non-local egress; otherwise ask the user to approve a human/current-session substitute.
5. Revise until the gate passes or `max_revisions` is reached.
6. Produce `delivery-N.md` in the workspace.
7. Run the delivery gate after each delivery.
8. Stop when all delivery criteria pass, a cap is reached, or the user stops the loop.
9. Keep `state.json` current with status, iteration, last gate, consent, and blockers.
10. Append a compact entry to `run-log.md` after every context read, model call, check, gate verdict, revision, blocker, and stop decision.
11. Compare each blocker against the previous blocker. If the same blocker repeats for the configured no-progress window, stop or ask for the configured human checkpoint instead of revising again.
12. Treat token and USD budgets as operator limits in this session: if exact accounting is unavailable, stop and ask before continuing when the loop appears likely to exceed them.

## Files

- Source spec: `loop.yaml`
- Human summary: `LOOP.md`
- Resolved spec: `loop.resolved.json`
- Workspace: `~/vibe-code/personal-blog-cms/looper-output`
- State file: `~/vibe-code/personal-blog-cms/looper-output/loop-workspace/state.json`
- Run log: `~/vibe-code/personal-blog-cms/looper-output/loop-workspace/run-log.md`

## Goal

Implement 4 P2 features for the personal-blog-cms Spring Boot + React app: (1) RSS/Atom feed for blog posts, (2) XML sitemap for public routes, (3) Newsletter email send to subscribers, (4) Comment system with nested replies. Each feature must follow TDD (RED → GREEN → REFACTOR), end with git commit, and be browser-verified.

## Definition Of Done

All 4 features shipped to main branch. Backend tests pass (target: 80+/80+). Frontend builds clean. Each feature has at least one curl/browser verify. README updated with new endpoints. No new bugs introduced.

## Context Sources

- Read file `~/vibe-code/personal-blog-cms/SPEC.md`
- Read file `~/vibe-code/personal-blog-cms/README.md`
- Read file `~/vibe-code/personal-blog-cms/backend/src/main/java/com/blog/cms/`

## Verification Criteria

- `backend-build-ok` programmatic: run `["bash", "-c", "cd ~/vibe-code/personal-blog-cms/backend && mvn clean package -DskipTests -q"]` and expect `exit_zero`
- `backend-tests-pass` programmatic: run `["bash", "-c", "cd ~/vibe-code/personal-blog-cms/backend && mvn test -q"]` and expect `exit_zero`
- `frontend-build-ok` programmatic: run `["bash", "-c", "cd ~/vibe-code/personal-blog-cms/frontend && npm run build"]` and expect `exit_zero`
- `rss-endpoint-valid` programmatic: run `["bash", "-c", "curl -s http://localhost:8080/api/public/feed.xml | xmllint --noout -"]` and expect `exit_zero`
- `sitemap-endpoint-valid` programmatic: run `["bash", "-c", "curl -s http://localhost:8080/api/public/sitemap.xml | xmllint --noout -"]` and expect `exit_zero`
- `newsletter-api-works` programmatic: run `["bash", "-c", "curl -X POST http://localhost:8080/api/admin/newsletter/send-preview -H 'Content-Type: application/json' -o /dev/null -w '%{http_code}' | awk '{exit ($1 == 200 || $1 == 401)}'"]` and expect `exit_zero`
- `comments-api-works` programmatic: run `["bash", "-c", "curl -s 'http://localhost:8080/api/public/posts?pageSize=1' | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get(\"data\",[{}])[0].get(\"id\",0))'"]` and expect `stdout_contains`
- `code-quality` judge rubric: Each feature implementation must (a) follow existing code patterns in the project, (b) have proper error handling, (c) include JavaDoc or comments on public APIs, (d) not introduce hardcoded values that should be config. Score PASS if all four hold; REVISE otherwise.

- `seo-correctness` judge rubric: RSS feed must follow Atom 2.0 or RSS 2.0 spec. Sitemap must follow sitemaps.org protocol. Both must include proper XML declaration and namespaces. Score PASS if compliant; REVISE otherwise.


## Council

- `reviewer-1` judge via `["telegram:997864855"]` (non-local; timeout 3600s)

## Gates

### plan_gate

- When: `before_each_feature`
- Policy: `revise_until_clean`
- Verdict source: `reviewer-1`
- Criteria: `code-quality, seo-correctness`
- Max revisions: `2`

### delivery_gate

- When: `after_each_feature`
- Policy: `revise_until_clean`
- Verdict source: `reviewer-1`
- Criteria: `backend-tests-pass, frontend-build-ok`
- Max revisions: `2`

## Loop Control

- Max iterations: `12`
- Budget: `{"tokens": 2000000, "usd": 0.5, "wall_clock_min": 240}`
- No-progress: `{"action": "human_checkpoint", "max_stalled_iterations": 2, "signals": ["same feature blocked 2 iterations", "test failure pattern repeats", "s\u1ebfp rejected same code twice"]}`
- Human checkpoints: `after_feature_1_plan, after_feature_2_delivery, after_feature_3_delivery, after_feature_4_delivery`
- Stop conditions:
  - all 4 features delivered and committed
  - max_iterations reached
  - sếp says stop
  - budget cap exceeded

## Execution Boundary

- Mode: `in_session`
- Isolation: `current_workspace`
- Side effects: `{"duplicate_action_check": true, "note": "Each feature requires s\u1ebfp approval before moving to next", "requires_approval": true}`

If the loop needs scheduled runs, child-agent lifecycle management, concurrency control, or restart-safe step retries, stop and tell the user this Looper spec should be handed to a durable orchestrator.

## Observability

- State file: `~/vibe-code/personal-blog-cms/looper-output/loop-workspace/state.json`
- Run log: `~/vibe-code/personal-blog-cms/looper-output/loop-workspace/run-log.md`
- Checkpoint granularity: `gate`

Use `state.json` for the latest resumable status and `run-log.md` for the append-only history of what happened.

## Privacy

- Before sending `plan, deliveries` to `human-reviewer`, confirm consent and apply redactions `.env, secrets/**, **/*.key, **/application*.yml`.

## Start Now

If the user asked to run now, begin at step 1 under Operator Instructions and keep going until a stop condition is reached.
