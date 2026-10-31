# Week 18 — Status pages & observability

## Public status page / multi-tenancy
**Q1. Why a subdomain (`acme.roost.local`) instead of `/status/acme`?**
Branding, per-tenant CDN caching, cookie scoping if web auth is added, and no tenant id leaking in the path.

**Q2. How does the public page resolve the tenant, and how is that different from authenticated requests?**
Public: a filter reads the `Host` header, extracts the slug, looks up the workspace. Authenticated: workspaceId comes from the JWT. Same scoping afterward.

**Q3. What's the worst bug this feature could have, and how do you prevent it?**
Leaking another tenant's — or your own unpublished — monitors. Every query is scoped by `workspaceId` and filtered to `published = true`; an integration test puts a monitor in A and asserts it's absent from B's page.

**Q4. Why default `published = FALSE`?**
Fail-safe: nothing goes public unless deliberately opted in.

## Actuator / metrics
**Q5. Counter vs Gauge vs Timer — give a Roost example of each.**
Counter = `roost.probes.executed` (monotonic). Gauge = `roost.incidents.open` (up/down, sampled). Timer = `roost.probes.duration` (durations + percentiles).

**Q6. Prometheus — push or pull?**
Pull. It scrapes `/actuator/prometheus` on a schedule; the app only exposes the endpoint. Pushgateway is for short-lived batch jobs.

**Q7. How do you avoid label/cardinality explosion?**
Never tag by high-cardinality values (monitor id, URL, user). Tag by bounded categories (outcome=up/down).

**Q8. Why not expose all actuator endpoints in prod?**
`env`, `heapdump`, `threaddump`, `configprops` leak secrets/internals. Expose the minimum and secure them.

## Logging
**Q9. Why JSON logs in prod?**
Aggregators index fields for structured search and alerting; plain text is grep-only.

**Q10. What is the MDC and when is it cleared?**
A per-thread key/value map stamped on every log line; cleared in the filter's `finally`, or a recycled thread leaks the prior request's id.

**Q11. Correlation id across `@Async` — the pitfall and fix?**
A new thread starts with an empty MDC. Fix with a `TaskDecorator` that copies the MDC onto the async thread.

## CI / Docker
**Q12. Why `-B` in CI?** Batch mode — non-interactive, clean logs.
**Q13. Do Testcontainers run in CI?** Yes, Docker is preinstalled on GitHub-hosted runners.
**Q14. Why a multi-stage Dockerfile?** Build stage has JDK+Maven; runtime ships only JRE+jar — smaller, faster, smaller attack surface.
**Q15. `depends_on` vs healthcheck?** Start order vs actual readiness — use healthchecks.
**Q16. Why two Kafka advertised listeners in compose?** Host and container clients reach the broker at different addresses; advertised listeners hand each the correct one.
