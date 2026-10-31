# W18 D5 — Multi-stage Dockerfile + full local stack

## Definition & Why
A production-shaped image and a one-command local stack. `docker compose up` brings Postgres, Redis, Kafka and the app online together — the exact shape `system-design/` (W20) will deploy to AWS. A **multi-stage** Dockerfile keeps the shipped image tiny by throwing away the build toolchain.

## Mental Model
Two rooms with a hatch between them. The **build room** has the full JDK + Maven (~hundreds of MB), compiles the jar, and passes *only the jar* through the hatch. The **runtime room** has just a JRE + the jar and nothing else — smaller surface, faster pulls, less to attack. Compose is the stage manager wiring the services together on one network, each addressable by service name (`postgres`, `kafka`).

## My Roost Code (today)
- `Dockerfile` — stage 1 `eclipse-temurin:25-jdk`: copy wrapper+pom, `dependency:go-offline` (cached layer), then `package -DskipTests`. Stage 2 `eclipse-temurin:25-jre`: copy the jar, create and run as a non-root `roost` user, `ENTRYPOINT java -jar`.
- `.dockerignore` — excludes `target/`, `.git/`, `notes/`, `*.md` from the build context.
- `docker-compose.yml` — adds the `app` service (built from the Dockerfile) with healthcheck-gated `depends_on`; env vars point it at the service-named infra. **Dual-listener Kafka** so the app works both on the host (`localhost:9092`) and as a container (`kafka:9092`).

## Interview Points
- **Multi-stage**: build image has JDK+Maven; runtime has JRE+jar only → much smaller, faster deploys, smaller attack surface.
- **`depends_on` ≠ ready**: it controls *start* order, not readiness. Use healthchecks (or app-level retries) for actual readiness — Roost gates the app on `service_healthy` for db/redis.
- **Non-root** container user: a compromise shouldn't get root.
- **Kafka advertised listeners**: the broker tells clients where to reconnect; host vs container clients need different addresses, hence two listeners.
- **One process per container**: independent restart/scale, per-container logs/metrics.

## Common Mistakes
- Single-stage image shipping the whole JDK + Maven cache.
- Trusting `depends_on` for readiness → app starts before Postgres accepts connections.
- One advertised Kafka listener → either host or containers can connect, not both.
- Running as root; fat build context (no `.dockerignore`).

## Quick Revision + Q&A
**Q: Why multi-stage?** Build needs JDK+Maven (~500MB); runtime needs only JRE+jar (~200MB). Smaller, safer, faster.
**Q: `depends_on` vs healthcheck?** Start order vs actual readiness — use healthchecks.
**Q: Why two Kafka listeners?** Host clients and container clients resolve the broker at different addresses; advertised listeners hand each the right one.

## Handoff
This compose + Dockerfile is the artifact `system-design/` Week 20 picks up to deploy to AWS (EC2 → RDS → ALB → ECS).
