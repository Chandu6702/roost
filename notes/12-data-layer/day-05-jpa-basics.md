# JPA Basics

**Week 12 · Day 5** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

JPA (Jakarta Persistence API) is the standard API used to map Java objects to relational database tables. In Roost, JPA lets `Workspace`, `Monitor`, and `ProbeResult` Java entities be persisted to PostgreSQL, while Spring Data JPA provides repository methods for querying them. Flyway remains responsible for database schema changes, while JPA/Hibernate maps the Java model to that schema and validates that they match.

## 2. Mental Model

Think of the data layer as:

```text
Java Entity
    ↓
JPA / Hibernate
    ↓
SQL
    ↓
PostgreSQL
```

For performance:

```text
SQL query
    ↓
PostgreSQL Query Planner
    ↓
Choose execution plan
    ├── Seq Scan
    └── Index Scan
```

An index is a possible shortcut, not an instruction to PostgreSQL.

A composite index such as:

```text
(monitor_id, probed_at DESC)
```

is ordered primarily by `monitor_id`, then by `probed_at` within each monitor.

The leftmost-prefix principle means the first column matters when determining which queries can effectively use the index.

## 3. My Roost Code

### Window-function query

Used PostgreSQL to retrieve the latest 10 probe results **for each monitor**:

```sql
SELECT *
FROM (
    SELECT
        pr.*,
        ROW_NUMBER() OVER (
            PARTITION BY pr.monitor_id
            ORDER BY pr.probed_at DESC
        ) AS row_num
    FROM probe_results pr
) ranked
WHERE row_num <= 10
ORDER BY monitor_id, probed_at DESC;
```

Key idea:

```text
PARTITION BY monitor_id
        ↓
number rows separately for each monitor
        ↓
ORDER BY probed_at DESC
        ↓
newest result gets row_num = 1
        ↓
WHERE row_num <= 10
```

### Flyway migration

File:

`src/main/resources/db/migration/V4__idx_monitors_workspace.sql`

Added:

```sql
CREATE INDEX idx_monitors_workspace_id
ON monitors(workspace_id);

CREATE INDEX idx_probe_results_monitor_probed_at
ON probe_results(monitor_id, probed_at DESC);
```

The migration was successfully applied by Flyway and both indexes were verified in PostgreSQL.

### EXPLAIN ANALYZE

Before adding the index, the monitor query used:

```text
Seq Scan on monitors
Rows Removed by Filter: 6
```

After adding the index, PostgreSQL still used `Seq Scan`.

This was expected because the table contained only 8 monitor rows. PostgreSQL's planner can correctly choose a sequential scan when scanning the small table is cheaper than using the index.

We also tested:

```sql
EXPLAIN ANALYZE
SELECT *
FROM probe_results
WHERE monitor_id = 3
ORDER BY probed_at DESC;
```

PostgreSQL again chose a sequential scan followed by a sort because the table was very small.

## 4. Interview Points

* `EXPLAIN ANALYZE` executes a query and shows the actual execution plan, including actual timing and row counts.
* `Seq Scan` reads the table sequentially; `Index Scan` uses an index to locate rows.
* Creating an index does **not** guarantee that PostgreSQL will use it. The query planner chooses the estimated cheapest plan.
* Small tables can legitimately use `Seq Scan` even when a suitable index exists.
* A composite index contains multiple columns, and their order matters.
* `(monitor_id, probed_at DESC)` is well suited to queries such as:
  `WHERE monitor_id = ? ORDER BY probed_at DESC`.
* The leftmost-prefix principle means the leading columns of a composite index are important for effective index usage.
* Indexes improve some reads but add storage and write-maintenance overhead, so every column should not automatically be indexed.
* A window function such as `ROW_NUMBER()` can solve the **top-N-per-group** problem.
* `PARTITION BY` divides rows into independent groups for the window calculation.

## 5. Common Mistakes

* Assuming an index must produce `Index Scan`.
* Treating `EXPLAIN` cost numbers as milliseconds.
* Assuming a composite index is useful simply because all its columns appear somewhere in a query.
* Forgetting that **column order matters** in a composite index.
* Confusing "latest 10 overall" with "latest 10 for each monitor".
* Creating indexes on every column without considering actual query patterns and write overhead.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

```text
Top N per group
    → ROW_NUMBER()
    → PARTITION BY group
    → ORDER BY ranking column
    → filter row number

EXPLAIN ANALYZE
    → see execution plan + actual execution information

Index
    → possible read shortcut
    → planner decides whether to use it

Composite index
    → column order matters
    → leftmost prefix matters
```

**Q&A**

**Q: Why did PostgreSQL use `Seq Scan` after we created an index?**
A: The `monitors` table had only 8 rows. PostgreSQL estimated that scanning the small table was cheaper than using the index.

**Q: Does creating an index guarantee an Index Scan?**
A: No. PostgreSQL's query planner chooses the execution plan it estimates to be cheapest.

**Q: What does `EXPLAIN ANALYZE` provide?**
A: It executes the query and reports the chosen execution plan together with actual execution information such as timing, rows, and loops.

**Q: What is a composite index?**
A: An index built using multiple columns, such as `(monitor_id, probed_at DESC)`.

**Q: Why does column order matter in a composite index?**
A: The index is organized according to the specified column order, so the leftmost columns determine which query patterns can efficiently use it.

**Q: What is the leftmost-prefix principle?**
A: A composite index is most naturally usable starting from its leading column(s). For `(monitor_id, probed_at)`, `monitor_id` is the leading column.

**Q: Why is `(monitor_id, probed_at DESC)` useful for latest results for one monitor?**
A: PostgreSQL can narrow to a particular `monitor_id` and have the matching `probed_at` values in descending order.

**Q: Why shouldn't every column be indexed?**
A: Indexes consume storage and must be maintained during inserts, updates, and deletes, so unnecessary indexes increase write overhead.

**Q: What problem does `ROW_NUMBER() ... PARTITION BY` solve here?**
A: It allows us to number rows independently within each monitor and then keep only the first N rows from each group.

**Key Takeaway:**
Indexes are **query-pattern-driven optimizations**, not guarantees of faster execution. Understand the query, understand the index column order, and use `EXPLAIN ANALYZE` to understand what PostgreSQL actually chose.
