# Week 15 — Security & Multi-Tenancy Interview Q&A

**Focus:** Spring Security, JWT, authorization, multi-tenant isolation, Hibernate filters, and production security.

## 1. Authentication, JWT & Authorization

### Q1. What is the difference between authentication and authorization?

**Answer:**

- **Authentication** verifies who the caller is.
- **Authorization** determines what the authenticated caller is allowed to do.

In Roost, registration and login establish the user's identity, while Spring Security and method-level authorization control access to protected endpoints.

### Q2. Why should passwords be hashed instead of encrypted or stored as plain text?

**Answer:**

Passwords should be stored using a purpose-built, adaptive password-hashing algorithm, such as BCrypt, Argon2, or PBKDF2. Hashing is designed to be one-way, while encryption is reversible with a key. Plain-text storage exposes every password if the database is compromised.

Never log passwords or return password hashes in API responses.

### Q3. Explain the JWT authentication flow in a Spring Boot REST API.

**Answer:**

1. The client submits credentials to the login endpoint.
2. The application authenticates those credentials.
3. On success, the application issues a signed JWT.
4. The client sends the token in the `Authorization: Bearer <token>` header.
5. A security filter validates the token and constructs the authenticated security context.
6. Spring Security evaluates access rules before allowing the request to reach the protected endpoint.

The application must reject invalid, expired, or improperly signed tokens.

### Q4. What information should a JWT contain, and what should never be placed in it?

**Answer:**

A JWT can contain claims such as `sub` (subject), `exp` (expiration), `iss` (issuer), `aud` (audience), and a role claim when appropriate.

Roost uses the user's email as the JWT subject and includes a role claim. A JWT is signed, not automatically encrypted, so passwords, password hashes, secrets, and sensitive personal data should not be placed in its payload.

### Q5. What does stateless authentication mean? How does JWT expiration help?

**Answer:**

With stateless API authentication, the server does not need to maintain an HTTP session for each authenticated client. Each request carries its bearer token.

Roost's JWT expires after 15 minutes. Expiration limits the lifetime of a stolen token, but it does not revoke an already-issued token immediately. Short-lived access tokens, secure storage, key rotation, and a deliberate refresh or reauthentication strategy are important production considerations.

### Q6. How does role-based authorization work with `@PreAuthorize`?

**Answer:**

`@PreAuthorize` evaluates an authorization expression before invoking a method. With `@EnableMethodSecurity`, a method can require a role, for example:

`@PreAuthorize("hasRole('ADMIN')")`

Spring's `hasRole('ADMIN')` convention normally checks for the authority `ROLE_ADMIN`. The application's JWT-to-authority conversion must map the token's role claim correctly. Roost's admin endpoint returned 403 for a MEMBER and 200 for an ADMIN.

## 2. Multi-Tenancy & Tenant Isolation

### Q7. What is multi-tenancy, and why is tenant isolation a security boundary?

**Answer:**

Multi-tenancy allows one application to serve multiple customers or organizations. Tenant isolation ensures that one tenant cannot read, modify, or delete another tenant's data.

A missing ownership check can expose customer data even when login and JWT validation work correctly. Isolation must be enforced on every relevant data-access path, not merely in the UI.

### Q8. Why must the server derive the tenant from the authenticated identity instead of trusting a request's `workspaceId`?

**Answer:**

A client can modify request parameters and submit another tenant's ID. This is a form of insecure direct object reference or broken object-level authorization.

Roost resolves the authenticated email to the user's current workspace. The requested `workspaceId` must never override that trusted ownership context. Where an endpoint accepts a workspace ID, the server must verify that it matches the authenticated user's authorized workspace.

### Q9. How does Roost determine which workspace belongs to the caller?

**Answer:**

The authenticated principal supplies the user's email. `CurrentWorkspaceService.getCurrentWorkspaceId(email)` looks up the user and resolves the associated workspace. The service uses that workspace ID to scope monitor operations.

The critical principle is that ownership comes from the authenticated identity and server-side data, not from a client-supplied owner ID.

### Q10. How do you test tenant isolation effectively?

**Answer:**

Create two independent users, each with a separate workspace and monitor. Authenticate both users and verify that:

- Each user can retrieve their own workspace and monitor.
- User A cannot retrieve User B's monitor.
- User B cannot retrieve User A's monitor.
- A caller cannot query another workspace by changing the workspace ID.
- Create, update, and delete operations cannot cross tenant boundaries.

Test negative cases explicitly. A successful login or a successful request to one's own data does not prove isolation.

### Q11. Why should tenant ownership be checked on update and delete operations, not just reads?

**Answer:**

Otherwise, a caller may be unable to read another tenant's monitor but still modify or delete it by guessing its ID.

Roost's update and delete methods first resolve the monitor through an ID-and-workspace-scoped lookup. Updates do not allow the monitor's workspace association to be reassigned through the ordinary update request.

### Q12. How should child records, such as probe results, be protected?

**Answer:**

Authorization must be enforced through the owning resource. Before returning probe results for a monitor, Roost verifies that the requested monitor belongs to the authenticated user's workspace.

This prevents a caller from accessing another tenant's child records merely by knowing a monitor ID. Where possible, database queries should also be scoped through the verified parent relationship.

### Q13. What is Hibernate's `@Filter`, and how does Roost use it?

**Answer:**

Hibernate's `@FilterDef` declares a named filter and its parameters. `@Filter` defines a condition that Hibernate applies to relevant entity queries when the filter is enabled on the current Hibernate session.

Roost defines `workspaceFilter` on `Monitor` with the condition `workspace_id = :workspaceId`. `MonitorService.tenantWorkspaceId(email)` resolves the caller's workspace and enables or updates the filter parameter.

The filter adds a persistence-layer restriction, but it is not a substitute for explicit authorization and tenant-scoped queries.

### Q14. Does declaring `@Filter` automatically isolate all monitor queries?

**Answer:**

No. A Hibernate filter generally must be enabled on the session executing the query. Declaring the annotation alone does not guarantee isolation.

Developers must ensure that the filter is enabled consistently, with the correct tenant parameter, for every relevant execution path. Queries, bulk operations, native SQL, other entity mappings, and association-loading behavior must be assessed according to the exact Hibernate APIs and query types in use.

### Q15. How do you prove that a Hibernate filter works independently of application-level ownership checks?

**Answer:**

Use a real database integration test:

1. Create monitors belonging to two different workspaces.
2. Enable the Hibernate filter for workspace A.
3. Run an entity query that does not explicitly include a workspace predicate.
4. Assert that only workspace A's monitor is returned.
5. Change the filter parameter to workspace B and repeat.

The query must execute in the same Hibernate session where the filter is enabled. Roost's focused integration test performs this check using PostgreSQL Testcontainers. The focused test and the full test suite passed.

### Q16. What are the limitations of relying only on Hibernate filters for tenant security?

**Answer:**

Filters are defense in depth, not a complete security architecture. They can be ineffective if not enabled, may not cover every query mechanism, and do not automatically authorize every operation on every entity.

Use server-derived tenant context, explicit ownership checks, tenant-scoped repository queries, appropriate database constraints, and integration tests. For stronger defense in depth, consider database-level row-level security or separate schemas/databases where justified by the threat model.

### Q17. What is the difference between HTTP 401, 403, and 404 in a multi-tenant API?

**Answer:**

- **401 Unauthorized:** The request lacks valid authentication.
- **403 Forbidden:** The caller is authenticated but does not have permission to perform the operation.
- **404 Not Found:** The requested resource does not exist or the API intentionally conceals its existence from the caller.

Roost returns 404 for inaccessible monitors through its ownership-scoped lookup. The choice to conceal resource existence should be consistent across endpoints and documented as part of the API's security design.

## 3. Exception Handling & Production Security

### Q18. Why did Roost need a specific `ResponseStatusException` handler?

**Answer:**

The global exception handler had a catch-all handler for `Exception`. Without a more specific handler, `ResponseStatusException` was being converted into HTTP 500 even when the exception represented an intended status such as 404.

Adding a dedicated handler preserves the intended HTTP status and returns a consistent Problem Details response. Unexpected exceptions should still be logged appropriately and handled without exposing internal implementation details.

### Q19. What additional security measures would you consider before deploying a multi-tenant JWT API to production?

**Answer:**

- Enforce HTTPS and securely store client tokens.
- Use adaptive password hashing and never log credentials or bearer tokens.
- Validate JWT signature, expiration, issuer, audience where applicable, and allowed algorithms.
- Establish a strategy for token expiration, revocation where needed, and signing-key rotation.
- Enforce authorization and tenant isolation on every data operation.
- Apply input validation, rate limiting, least-privilege database permissions, and secure configuration management.
- Log security-relevant events without exposing secrets or sensitive payloads.
- Test tenant isolation, authentication failures, authorization failures, and concurrent operations.

### Q20. What regression tests should protect the security behavior implemented in Week 15?

**Answer:**

Keep tests for registration and login, protected endpoint access, JWT validation, role-based authorization, workspace isolation, cross-tenant monitor access, filter behavior, and correct HTTP error responses.

Roost's existing integration suite uses PostgreSQL Testcontainers, while service and controller tests cover narrower behaviors. The full `./mvnw clean test` suite passed after the filter integration test was added. Any future changes to authentication, workspace resolution, monitor queries, or exception handling should preserve these tests.

## Quick Revision

- Authentication answers **who are you?**; authorization answers **what can you do?**
- A signed JWT is not necessarily encrypted.
- The server must derive tenant ownership from trusted authenticated identity and server-side data.
- Every read and write path must enforce tenant boundaries.
- Hibernate `@Filter` is a session-level mechanism that must be enabled correctly; it is not the only security control.
- Test cross-tenant denial as well as successful access.
- Use deliberate HTTP semantics and avoid turning expected client errors into 500 responses.
- A passing filter test proves that the tested filter behavior works; it does not by itself prove every application path is secure.

**Key takeaway:** Multi-tenant security is an end-to-end property of authentication, authorization, tenant resolution, persistence behavior, and regression testing—not a feature supplied by JWT or a Hibernate annotation alone.
