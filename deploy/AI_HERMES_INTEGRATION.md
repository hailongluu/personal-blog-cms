# AI / Hermes integration

The admin **Scheduled Tasks** page is backed by `ScheduledTasksService`, which
shells out to a host-installed agentic CLI called **`hermes`** (see
`backend/.../ScheduledTasksService.java`).

## How it works
- `ScheduledTasksService` invokes `hermes cron <list|run|pause|resume|delete>` via
  `ProcessBuilder`. Job IDs are validated against a strict regex (anti command-injection).
- The binary name defaults to `hermes` on `PATH`; override with the JVM system
  property `-Dhermes.binary=/abs/path/to/hermes`.

## Why it currently errors
The `blog-api` container image (eclipse-temurin JRE) does **not** include the
`hermes` binary, so the cron features log:
`java.io.IOException: Cannot run program "hermes": error=2, No such file or directory`.
This is non-fatal — the rest of the app works; only the Scheduled Tasks cron list/run fails.

## To enable it
Pick one:

1. **Install hermes into the image** — extend `backend/Dockerfile` to `COPY` the
   `hermes` binary into `/usr/local/bin/hermes` (and `chmod +x`), or `apt-get install`
   it if packaged. Then it's on `PATH` for the JVM.
2. **Mount from host** — bind-mount the host's `hermes` into the container in
   `docker-compose.yml` (`./bin/hermes:/usr/local/bin/hermes:ro`) and ensure it's executable.
3. **Point at an explicit path** — add `-Dhermes.binary=/data/bin/hermes` to `JAVA_OPTS`.

> hermes is the user's own agentic tool (the same one that ran the `looper-output`
> workflows); it is intentionally **not** vendored into this repo.

## AI-assisted content (future)
Hooks where an LLM could plug in (all optional, none wired yet):
- Auto-summary / TL;DR from `contentMarkdown` (store in `excerpt` / `metaDescription`).
- Tag suggestions from post body.
- Related-posts re-ranking (currently topic + recency; could use embeddings).

To add one, prefer the **latest Claude models** via the Anthropic API and gate it
behind an admin action so it's explicit and cost-controlled.
