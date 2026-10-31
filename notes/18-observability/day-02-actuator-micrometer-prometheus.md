# W18 D2 — Actuator + Micrometer + Prometheus

## Definition & Why
**Actuator** exposes operational endpoints (health, info, metrics) over HTTP. **Micrometer** is a vendor-neutral metrics facade (SLF4J-for-metrics); **Prometheus** is a pull-based time-series DB that scrapes `/actuator/prometheus` on a schedule. Together they answer "is Roost healthy, and how is it behaving *right now*?" — the difference between a project and something you'd actually operate.

## Mental Model
Micrometer meters come in three shapes:
- **Counter** — only goes up (probes executed). You graph its *rate*.
- **Gauge** — a dial that can move either way, sampled at scrape time (open incidents).
- **Timer** — records durations and gives you count + sum + percentiles (probe latency).
Prometheus doesn't receive pushes; it comes knocking every ~15s and reads whatever the dials say.

## My Roost Code (today)
- Dependency `micrometer-registry-prometheus`; exposed `health,info,metrics,prometheus` only (sensitive endpoints stay off).
- `roost.probes.executed` — Counter tagged `outcome=up|down`, incremented per probe in `ProbeTask`.
- `roost.probes.duration` — Timer recording each probe's elapsed nanos.
- `roost.incidents.open` — Gauge in `RoostMetrics` bound to `incidentRepository.countByStatus(OPEN)`, polled on scrape.
- `/actuator/health` and `/actuator/prometheus` permitted unauthenticated (for load-balancer and scraper); everything else behind auth.

## Interview Points
- **Counter vs Gauge vs Timer** — pick by shape of the quantity (monotonic / point-in-time / duration).
- **Pull vs push**: Prometheus pulls; the app just exposes an endpoint. Pushgateway is only for short-lived batch jobs that die before a scrape.
- **Label/cardinality explosion**: never tag by user id, monitor id, or URL — each distinct tag value is a new time series. Tag by bounded categories (up/down).
- Customise health with a `HealthIndicator` bean (DB, Redis, Kafka indicators ship for free).
- Don't expose `env`, `heapdump`, `threaddump` publicly — they leak secrets/internals.

## Common Mistakes
- Exposing `*` (all endpoints) in prod.
- High-cardinality tags → Prometheus memory blowup.
- Treating a gauge like a counter (gauges can decrease; use for "how many right now").
- Forgetting the scraper needs unauthenticated (or token) access to `/actuator/prometheus`.

## Quick Revision + Q&A
**Q: Which meter type for probe latency, probes-run, and open-incidents?**
A: Timer, Counter, Gauge respectively.

**Q: How does Prometheus get the data?**
A: It scrapes (pulls) `/actuator/prometheus` on an interval; the app never pushes.

**Q: How do you avoid cardinality explosion?**
A: Only use low-cardinality tags (outcome=up/down), never per-entity identifiers.

**Q: Why not expose all actuator endpoints?**
A: Several (env, heapdump, threaddump, configprops) reveal secrets or internals. Expose the minimum and secure them.
