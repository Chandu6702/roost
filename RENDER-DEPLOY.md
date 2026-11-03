# Deploying roost on Render

A step-by-step runbook. The `render.yaml` blueprint in the repo root creates
everything; this file explains the clicks and the two gotchas to watch for.

## What gets created

| Resource      | Type              | Purpose                                      |
|---------------|-------------------|----------------------------------------------|
| `roost-redis` | Key Value (free)  | Cache + login rate-limiter                   |
| `roost-api`   | Web, Docker (free)| Spring Boot backend                          |
| `roost-web`   | Static site       | React SPA; proxies `/api/*` to `roost-api`   |

Postgres is **not** on Render — it lives on Supabase (free, no 30-day expiry),
reached over the IPv4 session pooler. The blueprint leaves `DB_PASSWORD` unset
(`sync: false`); set it once in the dashboard after the first apply (see below).
Flyway builds the schema on first boot.

Kafka is **off** — the app runs on Postgres + Redis only. No broker to deploy.

## Prerequisites

1. The repo must be **public** (or grant Render access to the private repo).
   To make it public:
   ```bash
   gh repo edit Chandu6702/roost --visibility public --accept-visibility-change-consequences
   ```
2. A Render account linked to your GitHub.

## Deploy

1. In Render: **New → Blueprint**, pick the `roost` repo. Render reads
   `render.yaml` and shows the four resources. Click **Apply**.
2. First build takes a few minutes (Docker image for the API, `npm` build for
   the SPA). Watch the `roost-api` logs — on first boot Flyway runs the
   migrations against the fresh database, then the app reports healthy at
   `/actuator/health`.

## The one thing to check after the first deploy  ⚠️

Render names service URLs after the service. If `roost-api` was free, its URL is
`https://roost-api.onrender.com` and everything just works. If that name was
taken, Render adds a suffix (e.g. `roost-api-ab12.onrender.com`).

- Open **roost-api** and copy its real URL.
- If it is **not** exactly `https://roost-api.onrender.com`, open **roost-web →
  Settings → Redirects/Rewrites** and change the `/api/*` rule's destination to
  `https://<your-real-api-url>/*`. Save; the static site redeploys.

To verify the whole chain: open the `roost-web` URL, register an account, create
a monitor. If login works and data persists, the proxy + DB + Redis are all wired.

## Free-tier notes

- **Cold starts:** the free API sleeps after ~15 min idle; the first request
  then takes ~1 minute to wake. Fine for a portfolio demo; mention it if a
  reviewer hits a slow first load.
- **Postgres:** hosted on Supabase (free, no 30-day expiry). A free Supabase
  project pauses after ~1 week idle; resume it from the Supabase dashboard
  (data persists) if the API can't reach the DB after a long quiet spell.

## Env vars (already set by the blueprint)

| Var                    | Source                                  |
|------------------------|-----------------------------------------|
| `SPRING_PROFILES_ACTIVE` | `prod`                                |
| `JWT_SECRET`           | generated once by Render                |
| `DB_HOST/PORT/NAME/USER` | literal Supabase pooler values        |
| `DB_PASSWORD`          | **set manually** in the dashboard (`sync: false`) |
| `SPRING_DATA_REDIS_URL`| from `roost-redis` connection string    |

After the first apply, open **roost-api → Environment**, add `DB_PASSWORD` with
the Supabase database password, and save (the service redeploys).

`ROOST_KAFKA_ENABLED` is left unset → defaults to `false`. Nothing else needed.
