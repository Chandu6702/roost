# W19 — Polish, hardening & handoff

## What this week did
Turned a feature-complete codebase into something presentable and operable.

- **Feature completion (D1):** verified the end-to-end flow (register → workspace → monitor → probe → incident → webhook → public status). Confirmed onboarding is a clean 3-step flow and documented it.
- **Error-handling audit (D2):** no raw `RuntimeException`, `printStackTrace`, or `System.out` anywhere in `src/main`. Every 4xx returns a `ProblemDetail`; every 5xx logs ERROR with a full stack and returns a generic message (no internals leaked). Optimistic-lock and invalid-state-transition conflicts return 409; typed not-found exceptions return 404.
- **README + DESIGN (D3):** rewrote the README (what/why/stack/architecture Mermaid/run/API tour/roadmap) and wrote `docs/DESIGN.md` covering the six interesting problems (outbox, tenant isolation, probe scaling, state machine, webhook signing, operability) and known limitations.
- **Refactor (D4):** the biggest cleanup was replacing the stub W16 webhook auto-config service with a real HMAC-signing delivery service, so no dead learning-scaffold remains; the W16 custom-auto-configuration *pattern* is preserved as the wiring.
- **Consolidated recall (D5):** `interview-qa/phase-b-consolidated-qa.md` — 60+ questions grounded in Roost across Weeks 10–19.

## Handoff state
- Builds green (`./mvnw -B verify` with Docker up for Testcontainers/embedded Kafka).
- `docker compose up --build` brings the whole stack online.
- Ready for `system-design/` Week 20 to take the Dockerfile + compose to AWS.

## Interview framing
When asked "tell me about a project," lead with the **pipeline** (probe → incident → outbox → Kafka → signed webhook) and the **two hard guarantees**: reliable eventing (transactional outbox, at-least-once) and tenant isolation (defense in depth). Those two stories carry most of a system-design-flavoured backend interview.
