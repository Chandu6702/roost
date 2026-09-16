# JPA + First Entities + Flyway Migration

**Week 12 · Day 1** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

JPA (Jakarta Persistence API) is the standard Java persistence specification used to map Java objects to relational database tables. Hibernate is the JPA implementation used by Roost. On Day 1, the in-memory `Workspace` and `Monitor` domain models were moved toward database persistence. Flyway owns the database schema through versioned SQL migrations, while Hibernate validates that the Java entity mappings match the existing PostgreSQL schema.

This gives Roost a controlled database schema instead of allowing Hibernate to modify the production schema automatically.

## 2. Mental Model

Think of three layers:

```text
Flyway
   ↓
PostgreSQL schema
   ↑
Hibernate validates
   ↑
JPA Entities
```

Flyway creates and changes the database.

Hibernate maps Java entities to those tables and checks that the mapping is compatible.

For Roost:

```text
Workspace.java  ─────→  workspaces
Monitor.java    ─────→  monitors
```

The `monitors.workspace_id` foreign key connects a monitor to its workspace.

### Schema lifecycle

```text
V1__workspaces.sql
        ↓
workspaces table

V2__monitors.sql
        ↓
monitors table

Hibernate ddl-auto: validate
        ↓
Check entity ↔ schema compatibility
```

Hibernate does **not** own schema changes.

## 3. My Roost Code

### `src/main/resources/db/migration/V2__monitors.sql`

```sql
CREATE TABLE monitors (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    type VARCHAR(50) NOT NULL,
    interval_seconds INT NOT NULL DEFAULT 60,
    timeout_millis INT NOT NULL DEFAULT 5000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### `Workspace.java`

`Workspace` became a JPA entity with an ID, required fields, and a protected no-argument constructor required by JPA.

```java
@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    protected Workspace() {}
}
```

### `Monitor.java`

`Monitor` maps its Java fields to the `monitors` table. Its `workspace` field represents the database foreign key.

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "workspace_id", nullable = false)
private Workspace workspace;

@Column(nullable = false, length = 120)
private String name;

@Column(nullable = false, length = 2048)
private String url;
```

### Hibernate configuration

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

The application successfully started with Hibernate validating the Flyway-created schema.

## 4. Interview Points

- **Why `ddl-auto: validate` with Flyway?**
  Flyway owns schema changes. Hibernate should validate the mapping, not independently modify the database.

- **`IDENTITY` vs `SEQUENCE`?**
  `IDENTITY` relies on database-generated identity values such as PostgreSQL `BIGSERIAL`. `SEQUENCE` uses a database sequence and gives Hibernate more control over ID allocation and batching.

- **What does `@Column(nullable = false)` mean?**
  It expresses that the mapped column is non-nullable. With Flyway managing the schema, the actual database constraint comes from the migration.

- **Where can JPA annotations go?**
  They can be placed on fields or getters. Roost uses field access.

- **Why not records for JPA entities?**
  JPA entities generally need a no-argument constructor, mutable state, and proxy/lazy-loading support. Records are immutable and final, so they are better suited to DTOs than normal JPA entities.

- **Why does an entity need a protected no-argument constructor?**
  JPA needs to instantiate entities itself. The constructor can remain protected so application code is discouraged from creating incomplete entities through it.

## 5. Common Mistakes

- Using `ddl-auto: update` together with Flyway and allowing two systems to manage the schema.
- Forgetting that the entity must match the existing Flyway schema when using `validate`.
- Omitting a database column from the entity mapping.
- Forgetting the JPA no-argument constructor.
- Using a record as a normal JPA entity.
- Confusing Java field names such as `intervalSeconds` with database names such as `interval_seconds`.
- Forgetting that an existing Flyway table may contain constraints not obvious from the Java class.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

> Flyway owns the PostgreSQL schema. JPA/Hibernate maps Java entities to that schema and `ddl-auto: validate` checks that the mapping matches. Entities need an ID, persistence-friendly construction, and field mappings that correspond to the database.

**Q&A**

**Q: Who creates the `monitors` table?**
A: Flyway, through `V2__monitors.sql`.

**Q: Who checks that `Monitor` matches that table?**
A: Hibernate during application startup because `ddl-auto` is `validate`.

**Q: Why not `update`?**
A: It allows Hibernate to alter the schema, competing with Flyway's migration history.

**Q: What does `@GeneratedValue(IDENTITY)` do?**
A: It tells JPA that the database generates the primary-key value.

**Q: Why is `workspace_id` important?**
A: It is the foreign key connecting a monitor row to its workspace.

**Q: Why use `TIMESTAMPTZ` for `created_at`?**
A: It stores an instant in PostgreSQL with timezone-aware semantics, which maps well to Java's `Instant`.

**Q: Why is `slug` present in `Workspace`?**
A: The existing V1 schema requires `slug` to be `NOT NULL` and `UNIQUE`, so the entity must represent that persisted field.

**Key Takeaway:**
**Flyway owns schema evolution; JPA entities represent that schema; Hibernate validates the mapping.**
