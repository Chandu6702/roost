# Roost

[![CI](https://github.com/Chandu6702/roost/actions/workflows/ci.yml/badge.svg)](https://github.com/Chandu6702/roost/actions/workflows/ci.yml)

**Quiet watch over your services.** A multi-tenant uptime & status-monitoring platform: teams register HTTP monitors, Roost probes them on schedule, opens and auto-resolves incidents, fans out signed webhook notifications, and publishes a per-tenant public status page.

Built as the flagship of a 9-month Java/Spring Boot career-transition roadmap — every feature maps to a learning day in [DAILY-PLAN.md](DAILY-PLAN.md), but the result is a real, production-shaped application, not a tutorial toy.

## Stack

Java 25 · Spring Boot 4.1 · Spring Security (JWT resource server) · Spring Data JPA · PostgreSQL 17 · Flyway · Redis · Kafka · Micrometer/Prometheus · Testcontainers · Maven · Docker

## What it does

- **Monitors** — each workspace registers HTTP endpoints with an interval and timeout.
- **Probing** — a scheduler claims due monitors (safe to run multiple instances) and probes them asynchronously on **virtual threads**.
- **Incidents** — a DOWN probe opens an incident (deduped: one active per monitor); a recovery auto-resolves it. State machine: `OPEN → ACKNOWLEDGED → RESOLVED`.
- **Reliable events** — incident changes are written to a **transactional outbox** in the same commit, then relayed to Kafka (at-least-once) and consumed asynchronously.
- **Notifications** — the consumer fans out **HMAC-SHA256-signed** webhooks to each tenant's subscribers, with exponential-backoff retry.
- **Multi-tenancy** — strict row-level isolation (Hibernate `@Filter` + explicit workspace scoping); workspace A can never read B's data.
- **Public status page** — `GET /public/status` resolved per tenant from the `Host` subdomain, showing only *published* monitors.
- **Operability** — Prometheus metrics, JSON logs with correlation IDs, optimistic locking, JPA auditing, RFC 7807 error responses.
- **Abuse protection** — per-IP rate limiting on `POST /auth/login` (Redis fixed-window, 429 + `Retry-After`).

## Architecture

```mermaid
flowchart LR
    SCH[Scheduler<br/>claims due monitors] --> PT[ProbeTask<br/>@Async virtual thread]
    PT -->|HTTP| TARGET[(Monitored URL)]
    PT -->|save| PR[(probe_results)]
    PT -->|DOWN/UP| INC[IncidentService]
    INC -->|one commit| DB[(Postgres:<br/>incident + audit + outbox)]
    OBX[OutboxPublisher<br/>@Scheduled] -->|poll unpublished| DB
    OBX -->|publish| K[[Kafka roost.incidents]]
    K --> NC[NotificationConsumer]
    NC --> WH[WebhookService<br/>HMAC + retry]
    WH -->|POST signed| SUBS[(Tenant subscribers)]
```

Detection is transactional; everything after the outbox is asynchronous and retryable. See [docs/DESIGN.md](docs/DESIGN.md) for the interesting problems and how they're solved.

## Run it locally

Infra only (recommended for development — run the app from your IDE / `mvnw`):

```bash
docker compose up -d postgres redis kafka
```

```bash
./mvnw spring-boot:run
```

Or the whole stack (app included) in containers:

```bash
docker compose up --build
```

Swagger UI: `http://localhost:8080/swagger-ui.html` · Metrics: `http://localhost:8080/actuator/prometheus`

## API tour

```bash
# 1. Register, then log in (returns a JWT)
curl -s -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"me@acme.io","password":"password123"}'
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"me@acme.io","password":"password123"}' | jq -r .token)
```

```bash
# 2. Create your workspace (one per user)
curl -s -X POST localhost:8080/workspaces -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"Acme","slug":"acme"}'
```

```bash
# 3. Create a monitor, publish it to the status page, register a webhook
curl -s -X POST localhost:8080/monitors -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Prod API","url":"https://example.com","type":"HTTP","intervalSeconds":60,"timeoutMillis":5000}'
curl -s -X PUT "localhost:8080/monitors/1/published?value=true" -H "Authorization: Bearer $TOKEN"
curl -s -X POST localhost:8080/webhooks -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://my.app/hooks/roost","secret":"whsec_at_least_16_chars"}'
```

```bash
# Public status page (map acme.roost.local -> 127.0.0.1 in your hosts file first)
curl -s http://acme.roost.local:8080/public/status
```

## Layout

```
roost/
├── DAILY-PLAN.md        ← the learning plan (Weeks 10–19, day-by-day)
├── docs/DESIGN.md       ← design decisions & trade-offs
├── notes/               ← 6-section knowledge-base notes per topic
├── interview-qa/        ← weekly + phase-consolidated interview Q&A
├── docker-compose.yml   ← postgres · redis · kafka · app
├── Dockerfile           ← multi-stage, non-root runtime
└── src/main/java/com/chandu/roost/
    ├── controller/ service/ repository/ entity/ dto/ error/
    ├── config/ security/ probe/ messaging/ publicstatus/ webhook/ ratelimit/ aop/
```

## Roadmap

Deployment (Docker → EC2 → RDS → ALB → ECS, Prometheus/Grafana) is picked up by the sibling `system-design/` repo (Month 5). AI incident postmortems + semantic monitor grouping come in `AI/` (Month 7).

## License

See [LICENSE](LICENSE).
