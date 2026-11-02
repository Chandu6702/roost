# Roost — Frontend

A React + Vite + TypeScript SPA client for the Roost API. It's a **pure client**:
it holds the JWT and calls the stateless Spring Boot backend. No Node server tier —
in production it builds to static files (deployable to S3 + CloudFront, or served
behind the ALB).

## Screens

| Route | What |
|---|---|
| `/login`, `/register` | Email + password auth. Register logs you straight in. |
| `/` | Workspace bootstrap (create one on first use) → monitors list, add/publish/delete. |
| `/incidents` | Workspace incident feed; acknowledge open incidents. |
| `/status` | Preview of the public status page (`/public/status`). |

## How it talks to the API

`baseURL` is empty, so requests are same-origin and the **Vite dev proxy**
(`vite.config.ts`) forwards `/auth`, `/monitors`, `/workspaces`, `/incidents`
and `/public` to the backend. This avoids CORS entirely in dev. The JWT is
attached by an axios request interceptor; a `401` clears it and bounces to login.

## Run it

Start the backend first (from the repo root):

```bash
docker compose up -d postgres redis kafka
./mvnw spring-boot:run
```

Then the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open the printed URL (default `http://localhost:5173`). Register → create a
workspace → add a monitor.

### Config

Copy `.env.example` to `.env` to override:

- `VITE_API_TARGET` — backend origin (default `http://localhost:8080`).
- `VITE_PUBLIC_HOST` — a seeded workspace subdomain (e.g. `acme.roost.local`) so
  the `/status` page resolves a tenant locally. The public endpoint reads the
  tenant from the `Host` header, which the browser can't set, so the dev proxy
  forwards this value on `/public` requests.

## Build

```bash
npm run build   # tsc -b && vite build → dist/
```
