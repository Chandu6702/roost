# Deploying roost on Render

A step-by-step runbook. The `render.yaml` blueprint in the repo root creates
everything; this file explains the clicks and the two gotchas to watch for.

## What gets created

| Resource      | Type              | Purpose                                      |
|---------------|-------------------|----------------------------------------------|
| `roost-db`    | Postgres (free)   | Main database; Flyway builds the schema on boot |
| `roost-redis` | Key Value (free)  | Cache + login rate-limiter                   |
| `roost-api`   | Web, Docker (free)| Spring Boot backend                          |
| `roost-web`   | Static site       | React SPA; proxies `/api/*` to `roost-api`   |

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
   `/health`.

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
- **Postgres expiry:** a free Render Postgres is deleted ~30 days after it is
  created. For a long-lived demo, upgrade the DB to a paid instance or plan to
  recreate it.

## Env vars (already set by the blueprint)

| Var                    | Source                                  |
|------------------------|-----------------------------------------|
| `SPRING_PROFILES_ACTIVE` | `prod`                                |
| `JWT_SECRET`           | generated once by Render                |
| `DB_HOST/PORT/NAME/USER/PASSWORD` | from `roost-db`              |
| `SPRING_DATA_REDIS_URL`| from `roost-redis` connection string    |

`ROOST_KAFKA_ENABLED` is left unset → defaults to `false`. Nothing else needed.
