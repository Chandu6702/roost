# Week 15 — Day 5: Multi-Tenant Isolation

**Roadmap:** [DAILY-PLAN.md](../../DAILY-PLAN.md)

## 1. Definition & Why

**Multi-tenancy** is an architectural approach in which one application serves multiple tenants while keeping each tenant's data isolated from other tenants.

In Roost, each user owns a workspace, and monitors belong to workspaces. A user must only be able to access monitors and their associated data within their own workspace.

### Why it matters

- **Data confidentiality:** Users must not access another user's monitors or probe history.
- **Authorization:** Knowing a resource's ID must not grant permission to access it.
- **Data integrity:** A user must not move a monitor into another workspace by modifying a request.
- **Defense in depth:** Tenant isolation should be enforced at the service and persistence-query levels, rather than relying exclusively on the frontend.

### Roost's tenant boundary

The authenticated user's email identifies the current user. The application resolves that user's workspace and uses its ID to scope monitor operations.

The client must not be trusted to determine resource ownership by submitting a workspace ID.

## 2. Mental Model

Think of the authenticated user's workspace as the boundary around all accessible data.

```text
Authenticated request
        |
        v
JWT authentication
        |
        v
Extract authenticated user's email
        |
        v
Resolve the user's workspace ID
        |
        +---------------------------+
        |                           |
        v                           v
Explicit query scoping       Hibernate filter
        |                           |
        +-------------+-------------+
                      |
                      v
            Access tenant's monitors
                      |
                      v
          Verify ownership before
          accessing child records
```

The important distinction is that **the server determines ownership**.

For example, if User A belongs to Workspace A and User B belongs to Workspace B, User A must not be able to access User B's monitor—even if User A knows its ID.

### Two layers of isolation

1. **Explicit tenant scoping:** Repository queries include the current workspace ID. This is the primary application-level authorization boundary.
2. **Hibernate filter:** The `workspaceFilter` is enabled on the current Hibernate session and configured with the workspace ID. This provides additional defense in depth for entities to which the filter applies.

A Hibernate filter is not a replacement for explicit authorization checks. It must be configured correctly and enabled before the relevant query executes.

## 3. My Roost Code

### `src/main/java/com/chandu/roost/service/MonitorService.java`

**Resolve the tenant and configure the filter**

The `tenantWorkspaceId(email)` helper:

- Calls `CurrentWorkspaceService.getCurrentWorkspaceId(email)`.
- Unwraps the `EntityManager` into a Hibernate `Session`.
- Enables `workspaceFilter` with the current workspace ID, or updates its parameter if already enabled.
- Returns the workspace ID for explicit query scoping.

**Scope monitor lookup**

`getMonitor(id, email)` resolves the current workspace and uses:

```java
monitorRepository.findByIdAndWorkspaceId(id, workspaceId)
```

If no monitor belongs to that workspace with the requested ID, the service throws `MonitorNotFoundException`.

This is safer than `findById(id)` followed by an assumption that the requested ID is authorized.

**Prevent workspace reassignment**

`createMonitor(request, email)` obtains ownership from the authenticated user's workspace rather than trusting `request.workspaceId()`.

`updateMonitor(id, request, email)` first loads the monitor through the tenant-scoped lookup and updates its editable properties without changing its workspace.

**Protect related records**

`getProbeResults(monitorId, pageable, email)` verifies monitor ownership through `getMonitor()` before querying probe results.

This matters because child records such as `ProbeResult` are associated with a monitor. Checking the parent resource's ownership prevents a user from retrieving another tenant's probe history merely by supplying a monitor ID.

### Other relevant files

- `src/main/java/com/chandu/roost/service/CurrentWorkspaceService.java` — resolves the authenticated user's workspace.
- `src/main/java/com/chandu/roost/repository/MonitorRepository.java` — tenant-scoped monitor lookup and queries.
- `src/main/java/com/chandu/roost/service/MonitorServiceTest.java` — unit tests for monitor service behavior.
- `src/test/java/com/chandu/roost/integration/MonitorPersistenceIntegrationTest.java` — authenticated persistence and concurrent-write integration tests.

## 4. Interview Points

**Q1. What is multi-tenancy?**

Multi-tenancy allows one application instance to serve multiple users or organizations while isolating their data and access.

**Q2. Why is checking a resource ID insufficient for authorization?**

IDs identify resources; they do not establish that the requesting user owns or is allowed to access those resources. Queries must enforce the user's authorization boundary.

**Q3. Why should the server derive workspace ownership from the authenticated principal?**

Client-supplied ownership information can be manipulated. The server must derive the user's identity from the authenticated security context and resolve the authorized workspace independently.

**Q4. What is the difference between authentication and authorization?**

Authentication establishes who the user is. Authorization determines what that user may access or do.

**Q5. What is a Hibernate filter?**

A Hibernate filter is a parameterized restriction that can be enabled on a Hibernate session to constrain queries for mapped entities to which the filter applies.

**Q6. Is a Hibernate filter sufficient for tenant isolation?**

No. It must be correctly mapped, enabled, and parameterized. Explicit tenant-scoped queries and authorization checks should remain in place, particularly for sensitive resource access and child records.

**Q7. How should an API handle a request for another tenant's monitor?**

It should not disclose the monitor. Roost uses a tenant-scoped lookup and throws `MonitorNotFoundException` when the monitor is not found within the current workspace. The API can return 404 to avoid disclosing cross-tenant resource existence.

**Q8. How do you test tenant isolation?**

Create separate users and workspaces, authenticate as one user, and attempt to access the other user's resources. Verify that cross-tenant access is denied, including access to related records such as probe history.

## 5. Common Mistakes

- **Trusting `workspaceId` from the request:** A client can change it. Derive ownership from the authenticated user instead.
- **Using an unscoped lookup:** `findById(id)` alone does not establish tenant ownership.
- **Protecting monitors but forgetting their children:** Verify the parent monitor's ownership before querying its probe results.
- **Relying only on the Hibernate filter:** Keep explicit repository scoping and service-level authorization.
- **Changing workspace ownership during an ordinary update:** Update the monitor's permitted fields without silently reassigning its tenant.
- **Testing only with unauthenticated requests:** Tenant tests must exercise authenticated users and their actual workspace boundaries.

### Testing lesson from today's work

The service unit test initially failed with a `NullPointerException` because `EntityManager.unwrap(Session.class)` returned `null` from the mock.

The test was corrected by explicitly constructing `MonitorService` with the intended mocks and stubbing the Hibernate session and filter behavior.

This illustrates an important testing principle: when a unit test depends on constructor-injected collaborators, ensure the system under test receives the exact mock instances that the test configures.

## 6. Time Complexity

Tenant resolution and Hibernate filter configuration are generally constant-time operations, excluding database access.

For a tenant-scoped monitor lookup:

- **Expected lookup time:** Typically \(O(1)\) with a suitable database index on the lookup columns, such as monitor ID and workspace ID.
- **Probe history:** Depends on the database query plan, indexing, and page size. Pagination bounds the number of records returned but does not by itself guarantee an efficient query.

These are expected behaviors, not guarantees; verify actual performance with database indexes and query plans.

## 7. When to Use (Production)

Tenant isolation is essential for:

- SaaS monitoring platforms such as Roost.
- Multi-organization business applications.
- Shared databases serving multiple customers.
- APIs that expose resources by ID.
- Systems where parent resources have related records, events, logs, or history.

For stronger production guarantees, ensure every tenant-owned query is scoped appropriately, review bulk updates and deletes, and test cross-tenant access for all relevant endpoints. Where warranted, database-level row-level security can provide an additional enforcement layer.

## 8. Quick Revision

- The authenticated user determines the authorized workspace.
- Never trust a client-supplied workspace ID to establish ownership.
- Scope monitor queries by the current workspace ID.
- Enable and parameterize Hibernate filters correctly.
- Verify parent-resource ownership before retrieving child records.
- A resource ID is not proof of authorization.
- Test cross-tenant access using separate authenticated users.

## 9. Self-Check Q&A

**1. A user knows another user's monitor ID. Should that be sufficient to retrieve it?**

No. The monitor must belong to the requesting user's authorized workspace.

**2. Why does Roost use `findByIdAndWorkspaceId()`?**

It combines the resource identifier with the tenant boundary in the database lookup.

**3. What prevents a client from creating a monitor under an arbitrary workspace?**

The service derives the workspace from the authenticated user's email rather than using the request's workspace ID as the ownership source.

**4. Why verify ownership before fetching probe history?**

Probe results are associated with monitors and must not be exposed through a monitor ID belonging to another tenant.

**5. Does enabling a Hibernate filter eliminate the need for explicit scoping?**

No. The filter is defense in depth; explicit tenant-scoped access remains essential.

**6. Why did the unit test throw a `NullPointerException`?**

The mocked `EntityManager` returned `null` when unwrapping the Hibernate `Session`. Explicit construction and correct mock stubbing fixed the test setup.

## 10. Key Takeaway

**Tenant isolation must be enforced by the backend, not assumed from the request.** In Roost, the authenticated identity determines the workspace, explicit queries scope resources to that workspace, and ownership is checked before accessing related data. Hibernate filters provide an additional layer of protection.
