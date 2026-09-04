# First Flyway Migration

**Week 10 · Day 5** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

**Flyway** is a database migration tool that manages database schema changes as versioned SQL migrations. Instead of letting the application guess how the database schema should change, we explicitly define each change in a migration such as `V1__workspaces.sql`. Spring Boot automatically runs Flyway migrations during application startup, ensuring the database schema is brought to the expected version. This gives Roost a reproducible, reviewable database history across development, testing, staging, and production.

Today we connected Roost to PostgreSQL running in Docker and applied the first migration, creating the `workspaces` table.

---

## 2. Mental Model

Think of Flyway as **Git for the database schema**.

```text
Roost starts
     ↓
Spring Boot creates DataSource
     ↓
Flyway connects to PostgreSQL
     ↓
Checks flyway_schema_history
     ↓
Finds migrations that have not run
     ↓
Runs V1__workspaces.sql
     ↓
Records V1 in flyway_schema_history
     ↓
Application starts
```

The important idea:

> **Application code has versions. Database schema should have versions too.**

For example:

```text
V1__workspaces.sql
V2__add_monitors.sql
V3__add_probe_interval.sql
...
```

Flyway remembers which migrations have already been executed.

---

## 3. My Roost Code

### Docker PostgreSQL

`docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:17
    ports:
      - "5433:5432"
    environment:
      POSTGRES_USER: roost
      POSTGRES_PASSWORD: roost
      POSTGRES_DB: roost
```

**Key point:** PostgreSQL 18 is already running on my machine on port `5432`, so Roost's Docker PostgreSQL is exposed on host port `5433`.

```text
localhost:5433 → Docker PostgreSQL 17:5432
localhost:5432 → Existing PostgreSQL 18
```

### Spring datasource + Flyway

`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: roost

  datasource:
    url: jdbc:postgresql://localhost:5433/roost
    username: roost
    password: roost

  flyway:
    enabled: true
```

**Key point:** Spring Boot's DataSource connects Roost to the Docker PostgreSQL instance, and Flyway uses that DataSource.

### First migration

`src/main/resources/db/migration/V1__workspaces.sql`

```sql
CREATE TABLE workspaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Key point:** The default Flyway directory is `db/migration` (singular), not `db/migrations`.

### Result

Flyway reported:

```text
Successfully validated 1 migration
Successfully applied 1 migration to schema "public", now at version v1
```

The database now contains:

```text
public
├── flyway_schema_history
└── workspaces
```

I also verified the Flyway schema history and the created table.

---

## 4. Interview Points

* **What is Flyway?**
  A database migration tool that applies versioned, ordered schema changes.

* **Why is `flyway_schema_history` important?**
  Flyway uses it to record which migrations have already been applied, including their versions and migration metadata.

* **Why version database changes?**
  So every environment can reproduce the same schema evolution in the same order.

* **What happens when Spring Boot starts?**
  Flyway connects to the configured database, checks migration history, validates pending migrations, executes them, and records successful migrations.

* **Why prefer Flyway over `ddl-auto=update`?**
  Flyway makes schema changes explicit, versioned, reviewable, reproducible, and suitable for controlled production deployments.

* **Why not use JPA entities yet?**
  Roost's roadmap introduces JPA in a later week. Today's goal is to establish explicit database migrations first.

* **Why is Docker useful for the development database?**
  It gives the project a reproducible PostgreSQL environment without modifying the developer's existing PostgreSQL installation.

* **What is the difference between `5433:5432`?**
  `5433` is the host port; `5432` is the PostgreSQL port inside the Docker container.

---

## 5. Common Mistakes

* **Using `db/migrations` instead of `db/migration`** — Flyway's default migration location is `classpath:db/migration`. The directory must therefore be `db/migration` unless the location is explicitly customized.

* **Assuming `docker compose exec ... psql -h localhost` tests Windows localhost** — `localhost` in that command refers to the PostgreSQL container itself, not the Windows host. A host-side connection must be tested from Windows.

* **Stopping native PostgreSQL without checking its purpose** — My machine already uses PostgreSQL 18 for other development databases. Instead of disrupting that environment, Roost uses Docker host port `5433`.

* **Confusing Maven BUILD SUCCESS with Spring Boot startup success** — Maven can finish the `spring-boot:run` goal/process handling even when the application itself fails during startup. The Spring Boot logs must be checked for `Started RoostApplication`.

* **Timezone identifier mismatch** — Java was supplying `Asia/Calcutta` during the PostgreSQL connection startup, while the PostgreSQL instance rejected that identifier. Running the JVM with `-Duser.timezone=Asia/Kolkata` resolved the connection.

---

## 6. Quick Revision + Q&A

**Revision (30 sec):**

Flyway manages database schema changes as versioned migrations. When Roost starts, Flyway checks `flyway_schema_history`, validates pending migrations, executes them in order, and records successful migrations. Our first migration, `V1__workspaces.sql`, created the `workspaces` table in Docker PostgreSQL.

### Q&A

**Q1. What does Flyway do?**

**A.** Flyway manages database schema evolution using versioned migrations. It applies migrations in order and records their execution history.

---

**Q2. What is `flyway_schema_history`?**

**A.** It is Flyway's metadata table that records which migrations have been applied to the database.

---

**Q3. What does `V1__workspaces.sql` mean?**

**A.** `V1` is the migration version, `__` separates the version from the description, and `workspaces` describes what the migration does.

---

**Q4. What happens if I restart Roost after V1 has already run?**

**A.** Flyway checks `flyway_schema_history`, sees that V1 has already been applied, and does not execute it again.

---

**Q5. What does Flyway provide that `ddl-auto=update` does not?**

**A.** Flyway provides explicit, versioned, ordered, reviewable, and reproducible schema changes. `ddl-auto=update` derives schema changes from JPA mappings and is not a reliable mechanism for controlled production schema evolution.

---

**Q6. Why is `ddl-auto=update` risky for production?**

**A.** It automatically attempts to modify the schema based on entity changes. It does not provide the same explicit migration history or controlled handling of operations such as renames, destructive changes, indexes, or data migrations.

---

**Q7. Why are we using port 5433 instead of 5432?**

**A.** PostgreSQL 18 is already running natively on Windows and uses port 5432. Roost's Docker PostgreSQL therefore uses host port 5433 while PostgreSQL inside the container continues to listen on 5432.

---

**Q8. Does `5433:5432` mean PostgreSQL itself changed to port 5433?**

**A.** No. PostgreSQL inside the container remains on 5432. Docker maps host port 5433 to container port 5432.

---

**Q9. Why did the original `psql -h localhost` test not prove Spring was connecting to Docker PostgreSQL?**

**A.** Because the command was executed with `docker compose exec`, so it ran inside the container. There, `localhost` referred to the container itself. It did not test Windows' `localhost`.

---

**Q10. What caused the `Asia/Calcutta` error?**

**A.** During the PostgreSQL connection startup, the Java/PostgreSQL client supplied the JVM's timezone as `Asia/Calcutta`. The PostgreSQL instance rejected that timezone identifier. Setting the JVM timezone explicitly to `Asia/Kolkata` resolved the connection.

---

**Q11. How do we know Flyway actually ran?**

**A.** Spring Boot logged that one migration was successfully validated and applied, and the database contains `flyway_schema_history` with the V1 migration recorded.

---

**Q12. What is the role of Docker in this setup?**

**A.** Docker provides an isolated PostgreSQL 17 instance for Roost. It allows Roost to have its own reproducible database without interfering with the existing PostgreSQL 18 environment used for other projects.

---

**Q13. Why should migration files be treated like application code?**

**A.** Schema changes affect application behavior and production data. They should therefore be version-controlled, code-reviewed, tested, and deployed predictably.

---

**Q14. What is the default Flyway migration location in Spring Boot?**

**A.** `classpath:db/migration`, which corresponds to `src/main/resources/db/migration` in a standard Maven project.

---

**Q15. What does `spring.flyway.enabled: true` do?**

**A.** It enables Flyway's Spring Boot integration so Flyway runs automatically during application startup.

---

**Q16. What database did Roost connect to today?**

**A.** PostgreSQL 17 running in Docker, database `roost`, exposed to the host at `localhost:5433`.