# W17 D4 — JPA auditing + `@Version` optimistic locking

## Definition & Why
**Auditing**: Spring Data JPA fills `created_at / updated_at / created_by / updated_by` automatically via an `AuditingEntityListener`, so every row records when and by whom it changed — free provenance for debugging and compliance.
**Optimistic locking**: instead of locking a row while a user thinks, we let everyone read freely and detect conflicts *at write time* using a `@Version` column. Cheap when contention is low, which is the common case.

## Mental Model
`@Version` is a row's edit counter. Every read carries the counter; every update says "update … **where version = the one I read**" and bumps it. If someone else wrote first, the version moved, zero rows match, Hibernate throws `ObjectOptimisticLockingFailureException`. It's "last reader to write loses" rather than "first to grab a lock wins".
`AuditorAware` is the question "who is acting right now?" — answered from the security context, or "system" for background jobs.

## My Roost Code (today)
- `Auditable` `@MappedSuperclass` with the four audit columns + `@EntityListeners(AuditingEntityListener.class)`.
- `Incident extends Auditable` and gains `@Version Long version`.
- `JpaAuditingConfig` — `@EnableJpaAuditing` + an `AuditorAware<String>` that reads the authenticated email, falling back to `"system"` (probe-triggered incidents have no logged-in user).
- `V12__incident_auditing_version.sql` — adds the columns and backfills existing rows from `opened_at/resolved_at`.
- Hardened the error layer so conflicts surface correctly: `ObjectOptimisticLockingFailureException` → **409**, invalid state transitions (`IllegalStateException` from the incident state machine) → **409**, and `IncidentService` now throws typed `IncidentNotFoundException` / `MonitorNotFoundException` (→ 404) instead of raw `RuntimeException` (which was becoming a silent 500). The 500 catch-all now logs the full stack at ERROR.

## Interview Points
- **Optimistic vs pessimistic**: optimistic = no locks, conflict detected at commit (low contention, e.g. editing an incident). Pessimistic = `SELECT … FOR UPDATE` holds the row (high contention, e.g. seat/inventory).
- SQL mechanism: `UPDATE … SET …, version = version + 1 WHERE id = ? AND version = ?`; zero rows updated → conflict.
- Correct HTTP status for a version clash is **409 Conflict** with "reload and retry".
- Keep `AuditorAware` separate from the entity so you can swap the actor in tests.
- Audit trail + append-only log (outbox) give an immutable history; lock down the app DB user to INSERT-only on true audit tables.

## Common Mistakes
- Expecting a lock to be *held* — optimistic locking holds nothing; it only detects.
- Returning 500 on a version conflict. It's a client-recoverable 409.
- Reading `SecurityContext` directly inside `@PrePersist` — couples auditing to security and breaks under async (no context on the probe thread). `AuditorAware` + a "system" fallback fixes both.
- Forgetting the version column exists in the DB (added by migration) → `ddl-auto: validate` fails at boot.

## Quick Revision + Q&A
**Q: What does `@Version` do at the SQL level?**
A: Adds `AND version = ?` to the update and increments it. No matching row ⇒ someone else won ⇒ exception.

**Q: Why not read the current user in a `@PrePersist` callback directly?**
A: It couples the entity to Spring Security and can't run on a thread with no security context (async probes). `AuditorAware` is injectable and testable.

**Q: When would you choose pessimistic locking instead?**
A: High-contention hot rows where retrying optimistic failures would thrash — e.g. decrementing limited inventory. Hold the row with `SELECT … FOR UPDATE`.

**Q: Auditing created_by for a scheduled job — what gets stored?**
A: "system" — the fallback, since there's no authenticated principal on the scheduler thread.
