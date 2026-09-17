# JPA Entity Relationships

**Week 12 · Day 3** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

JPA entity relationships map relationships between Java entities to foreign-key relationships in the database. In Roost, a `Workspace` can have many `Monitor` entities, and each `Monitor` can have many `ProbeResult` entities. JPA lets us represent these relationships using `@OneToMany` and `@ManyToOne` instead of manually managing the foreign keys in application code.

The **owning side** is the side that contains the foreign-key mapping. The inverse side uses `mappedBy` to point to that relationship.

## 2. Mental Model

Think of the database first:

```text
workspaces
    │
    │  workspace_id
    ▼
monitors
    │
    │  monitor_id
    ▼
probe_results
```

Then map that structure into Java:

```text
Workspace
    │
    └── List<Monitor>

Monitor
    │
    ├── Workspace
    └── List<ProbeResult>

ProbeResult
    │
    └── Monitor
```

The key rule:

**The side containing the foreign key owns the relationship.**

For example, `monitors.workspace_id` is the foreign key, so `Monitor` owns the `Workspace` relationship.

`Workspace` uses:

```java
@OneToMany(mappedBy = "workspace")
private List<Monitor> monitors = new ArrayList<>();
```

`mappedBy = "workspace"` refers to the `workspace` field inside `Monitor`.

We explicitly use `LAZY` on `@ManyToOne` so related entities aren't automatically loaded unless needed.

## 3. My Roost Code

### `Workspace.java`

```text
src/main/java/com/chandu/roost/workspace/Workspace.java
```

Representative relationship mapping:

```java
@OneToMany(mappedBy = "workspace")
private List<Monitor> monitors = new ArrayList<>();
```

### `Monitor.java`

```text
src/main/java/com/chandu/roost/monitor/Monitor.java
```

Representative mapping:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "workspace_id", nullable = false)
private Workspace workspace;

@OneToMany(mappedBy = "monitor")
private List<ProbeResult> probeResults = new ArrayList<>();
```

### `ProbeResult.java`

```text
src/main/java/com/chandu/roost/probe/ProbeResult.java
```

Representative mapping:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "monitor_id", nullable = false)
private Monitor monitor;

public void setMonitor(Monitor monitor) {
    this.monitor = monitor;
}
```

### Relationship helper

`Monitor.addProbeResult()` keeps both sides of the relationship synchronized:

```java
public void addProbeResult(ProbeResult probeResult) {
    probeResults.add(probeResult);
    probeResult.setMonitor(this);
}
```

### Database migration

```text
src/main/resources/db/migration/V3__probe_results.sql
```

The migration creates `probe_results` with a foreign key to `monitors`.

### Verification

```text
src/test/java/com/chandu/roost/probe/ProbeResultRepositoryTest.java
```

The persistence test successfully verified:

```text
Workspace
├── Monitor 1
│   ├── ProbeResult 1
│   ├── ProbeResult 2
│   └── ProbeResult 3
└── Monitor 2
    ├── ProbeResult 4
    └── ProbeResult 5
```

## 4. Interview Points

* The **owning side** is the side that manages the foreign-key relationship.
* In a `Workspace` → `Monitor` relationship, `Monitor` owns it because `monitors.workspace_id` is the FK.
* `mappedBy` marks the inverse side and refers to the Java field on the owning entity.
* `@ManyToOne` defaults to `EAGER`, so explicitly using `LAZY` is generally preferable for control over database loading.
* `@OneToMany` defaults to `LAZY`.
* A helper method such as `addProbeResult()` keeps both sides of a bidirectional relationship synchronized.
* `LazyInitializationException` can occur when a lazy relationship is accessed after the persistence context has closed.
* `orphanRemoval = true` is different from `cascade`; orphan removal deletes a child when it is removed from the parent's relationship collection.

## 5. Common Mistakes

* Assuming the side with "many" automatically owns the relationship. The actual rule is: **the side with the foreign key owns it**.
* Forgetting `mappedBy` on the inverse side.
* Updating only one side of a bidirectional relationship and leaving the Java object graph inconsistent.
* Accidentally using eager loading everywhere and causing unnecessary database queries.
* Thinking `LazyInitializationException` means a database column is missing. It actually relates to accessing a lazy association after the persistence context has closed.

**Genuine self-check corrections:**

* Owning side → determined by the FK, not simply because multiple monitors belong to one workspace.
* `LAZY` → primarily about controlling when related entities are loaded.
* `LazyInitializationException` → caused by accessing a lazy association when the persistence context is no longer available.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

> JPA relationships map object relationships to database foreign keys. The entity containing the FK is the owning side. `mappedBy` identifies the inverse side. Use `LAZY` to avoid automatically loading relationships, and keep both sides synchronized when using bidirectional relationships.

**Q&A**

**Q: Which side owns a JPA relationship?**
A: Usually the side containing the foreign key.

**Q: Why does `Workspace.monitors` use `mappedBy = "workspace"`?**
A: Because the `workspace` field in `Monitor` owns the relationship.

**Q: Why use `FetchType.LAZY` on `Monitor.workspace`?**
A: To avoid loading the workspace automatically whenever a monitor is loaded.

**Q: What is `LazyInitializationException`?**
A: An exception that can occur when accessing a lazy association after the persistence context has closed.

**Q: Why use helper methods for bidirectional relationships?**
A: To keep both sides of the in-memory relationship synchronized.

**Q: What does the database relationship look like?**
A: `workspaces → monitors → probe_results`, connected through foreign keys.

**Key Takeaway:**

> **Foreign key → owning side → `mappedBy` on inverse side → control loading with `LAZY`.**
