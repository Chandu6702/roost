# Roost

**Quiet watch over your services.** A multi-tenant uptime & status monitoring platform — teams register monitors, Roost probes them on schedule, opens incidents when things break, fans out notifications, and publishes a per-tenant public status page.

Built as the flagship of a 9-month career-transition roadmap (Java Spring Boot, Months 3–4). Every feature ships as part of a learning day — see [DAILY-PLAN.md](DAILY-PLAN.md).

## Stack

Java 25 · Spring Boot 4.1 · Spring Security · Spring Data JPA · PostgreSQL 17 · Flyway · Redis (W16+) · Kafka (W17+) · Testcontainers · Maven

## Status

Bootstrapped — Weeks 10–19 of active development. See DAILY-PLAN.md for progress.

## Layout

```
roost/
├── DAILY-PLAN.md        ← the plan (Weeks 10–19, day-by-day)
├── CLAUDE.md            ← mentor session scripts
├── mistakes.md          ← trap log
├── notes/               ← 6-section notes per topic
├── interview-qa/        ← weekly interview Q&A banks
├── reviews/             ← weekly self-assessments
├── templates/
└── src/
    ├── main/java/com/chandu/roost/
    └── test/java/com/chandu/roost/
```

## Run locally (once Week 10 lands)

Prerequisites: JDK 25 (pom targets Java 25), Docker (for Postgres).

```bash
docker compose up -d db
./mvnw spring-boot:run
```

More detailed run instructions land in W12 (once Postgres is wired) and W18 (once the full docker-compose stack ships).
