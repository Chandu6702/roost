# Spring Boot Caching with Redis

**Week 16 · Day 4 · [DAILY-PLAN](../../DAILY-PLAN.md)**

## 1. Definition & Why

**Caching** stores the result of an operation so repeated requests can reuse it instead of repeating the same work.

**Spring Cache Abstraction** provides annotations and a common API for caching method results. **Redis** is an in-memory data store that can act as a shared cache across application instances.

In Roost, retrieving a workspace's monitor list repeatedly can cause unnecessary PostgreSQL queries. Caching can reduce those reads when the same workspace is requested frequently.

Important annotations:

- `@EnableCaching` — enables annotation-driven caching.
- `@Cacheable` — returns a cached result when available; otherwise, executes the method and caches its result.
- `@CacheEvict` — removes cached entries when data changes.

## 2. Mental Model

```text
GET workspace monitors
          |
          v
MonitorController
          |
          v
MonitorService
          |
          v
Authenticate user's workspace
Validate workspace access
          |
          v
WorkspaceMonitorCacheService
          |
          v
Check Redis cache
       /       \
    HIT         MISS
     |            |
Return cached   Query PostgreSQL
result          Convert entities to DTOs
                    |
                    v
                Cache result
                    |
                    v
                Return DTOs
```

When a monitor is created, updated, or deleted, the cache eviction annotation removes entries so later reads can retrieve fresh data.

**Security principle:** workspace authorization must happen before the cached service call. A cache key is not an authorization mechanism.

## 3. My Roost Code

### 3.1 Enable Spring caching

**Path:** `src/main/java/com/chandu/roost/RoostApplication.java`

- Added `@EnableCaching`.
- Enables Spring's annotation-driven caching infrastructure.

### 3.2 Configure Redis

**Paths:**
- `pom.xml`
- `docker-compose.yml`
- `src/main/resources/application.yml`

Added dependencies:

- `spring-boot-starter-cache`
- `spring-boot-starter-data-redis`

Added Redis 7 to Docker Compose and configured the host port mapping:

```yaml
ports:
  - "6380:6379"
```

Roost connects to `localhost:6380` because host port `6379` was already occupied. PostgreSQL remains on host port `5433`; its existing database configuration must be preserved.

Verified that the Redis container responds to `PING` with `PONG`.

### 3.3 Create the cache service

**Path:** `src/main/java/com/chandu/roost/service/WorkspaceMonitorCacheService.java`

- Created a separate Spring service for the cacheable method.
- Uses the cache name `workspaceMonitors`.
- Uses `workspaceId` as the cache key.
- Queries `MonitorRepository` on a cache miss.
- Converts entities to DTOs before returning the result.

Representative annotation:

```java
@Cacheable(
    cacheNames = "workspaceMonitors",
    key = "#workspaceId"
)
```

A separate bean allows the call to pass through Spring's caching proxy. This avoids the common self-invocation problem with proxy-based annotations.

### 3.4 Create a cache DTO

**Path:** `src/main/java/com/chandu/roost/dto/MonitorCacheDto.java`

Created a Java record containing:

- `id`
- `name`
- `url`
- `type`
- `intervalSeconds`
- `timeoutMillis`
- `createdAt`

Added `MonitorCacheDto.from(Monitor monitor)` to map a JPA entity into the DTO.

The DTO avoids directly caching the entity's lazy relationships and Hibernate proxy graph.

### 3.5 Integrate caching with the service

**Path:** `src/main/java/com/chandu/roost/service/MonitorService.java`

Updated `getMonitorsByWorkspace(...)` to:

1. Resolve the authenticated user's workspace.
2. Validate the requested workspace.
3. Call `WorkspaceMonitorCacheService` only after authorization.

Added `@CacheEvict(cacheNames = "workspaceMonitors", allEntries = true)` to monitor creation, updating, and deletion methods.

This invalidates every entry in the named cache. It is straightforward for the learning exercise, but can cause unnecessary cache misses for unrelated workspaces.

### 3.6 Preserve the existing API response

**Path:** `src/main/java/com/chandu/roost/controller/MonitorController.java`

- Updated the workspace-monitor endpoint to receive `List<MonitorCacheDto>`.
- Maps the DTO fields to the existing `MonitorResponse`.
- Keeps the external API response separate from the cache representation.

### 3.7 Tests

**Paths:**
- `src/test/java/com/chandu/roost/service/RedisCacheIntegrationTest.java`
- `src/test/java/com/chandu/roost/service/MonitorServiceTest.java`
- `src/test/java/com/chandu/roost/controller/MonitorControllerTest.java`

- Verified basic Redis string storage and retrieval.
- Updated the service test for the DTO return type.
- Fixed the MVC test context issue caused by caching being enabled without a `CacheManager` bean in the test slice.
- Confirmed the full test suite passes.

**Verification boundary:** the Redis test proves basic cache storage and retrieval for strings. It does not prove end-to-end serialization of `List<MonitorCacheDto>`, actual cache hits, or mutation-driven eviction against Redis. Those still require a dedicated integration test.

## 4. Interview Points

- **Spring Cache Abstraction:** provides a consistent API for caching method results.
- **`@EnableCaching`:** activates annotation-driven caching.
- **`@Cacheable`:** returns an existing value or executes the method and caches its result on a miss.
- **`@CacheEvict`:** invalidates cached entries.
- **Redis:** provides a cache that can be shared across application instances.
- **Cache key:** identifies a particular cached result; Roost uses the workspace ID.
- **DTO vs JPA entity:** DTOs provide a controlled serialization boundary and avoid caching Hibernate entity graphs.
- **Separate cache service:** lets calls pass through Spring's caching proxy.
- **Tenant isolation:** authorization must happen before cached data is returned.
- **Cache invalidation:** writes must invalidate or update affected entries to avoid stale reads.
- **`allEntries = true`:** simple but invalidates unrelated workspace entries too.
- **Test coverage:** a passing Redis string test does not prove the full DTO caching flow works.

## 5. Common Mistakes

- Assuming application startup proves Redis caching works.
- Enabling caching without a suitable `CacheManager`.
- Assuming a string-cache test proves DTO-list serialization works.
- Caching JPA entities without considering lazy relationships and Hibernate proxies.
- Performing cache access before authorization.
- Forgetting to invalidate cached results after writes.
- Using `assertSame` when serialized and deserialized objects are expected to be different instances.
- Calling a cacheable method through `this` and bypassing the Spring proxy.
- Evicting all cache entries without considering the effect on unrelated tenants.
- Accidentally removing existing PostgreSQL configuration while adding Redis.

No separate user-mistake entry is necessary unless a mistake is genuinely recurring.

## 6. Time Complexity

Caching does not automatically make every operation constant time. The actual cost depends on the cache backend, the key, the serialized value size, and the work performed on a miss.

- **Cache lookup:** typically close to O(1) average for a key lookup, excluding network and serialization overhead.
- **Redis read/write:** depends on the operation and value size; network latency and serialization also contribute.
- **Cache hit:** avoids the repository query and entity-to-DTO conversion performed by the cacheable method on a miss.
- **Cache miss:** includes the PostgreSQL query, entity-to-DTO mapping, and cache write.
- **DTO conversion:** O(n) for a list of n monitors.
- **`allEntries = true` eviction:** invalidates every entry in the named cache; the cost depends on the cache provider and its implementation.

These are conceptual costs, not benchmarks of Roost's current implementation.

## 7. When to Use (Production)

Use caching when:

- The same data is requested frequently.
- Database reads are expensive or contribute materially to response latency.
- The data can tolerate a clearly defined staleness window.
- Cache invalidation can be tied to the relevant write operations.

Production considerations:

- **Serialization:** configure and test Redis serialization for the exact cached DTO types.
- **Invalidation:** prefer targeted eviction when the affected workspace key is known and targeted invalidation is practical.
- **Tenant isolation:** always authorize before returning cached data; ensure keys and data scope are appropriate.
- **Cache stampedes:** consider protection when many concurrent misses can trigger expensive duplicate queries.
- **Failure handling:** define what the application should do when Redis is unavailable.
- **Memory and TTL:** choose an appropriate expiration policy and monitor cache memory.
- **Observability:** measure hit/miss rates, latency, eviction behavior, and Redis errors.
- **Testing:** test the actual serialized values and invalidation behavior against Redis, not only string storage.

For Roost, this is a reasonable learning implementation. It should not be considered production-verified until DTO-list serialization, cache hits, eviction, and Redis failure behavior have been tested.

## 8. Quick Revision

```text
@EnableCaching
    → Enables annotation-driven caching

@Cacheable
    → Cache hit: reuse value
    → Cache miss: execute method and cache result

@CacheEvict
    → Remove cached entries

workspaceMonitors
    → Cache name

workspaceId
    → Cache key

MonitorCacheDto
    → Controlled representation for cached results

MonitorService
    → Authorizes before accessing cache

Redis
    → Shared cache backend

PostgreSQL
    → Source of persisted monitor data
```

## 9. Q&A

**Q1. What is caching, and why use it?**

Caching reuses the result of an earlier operation to avoid repeating expensive work, such as querying PostgreSQL for frequently requested monitor lists.

**Q2. What does `@Cacheable` do on a cache hit and a cache miss?**

On a hit, Spring returns the cached value without executing the method body. On a miss, the method executes and its result is cached.

**Q3. What is the purpose of `@CacheEvict`?**

It removes cached entries so later requests can retrieve updated data instead of stale results.

**Q4. Why use Redis instead of a local in-memory cache?**

Redis can be shared by multiple application instances. A local in-memory cache belongs to an individual process.

**Q5. Why use a DTO instead of caching a JPA entity?**

A DTO provides a predictable data representation and avoids serialization problems associated with lazy relationships, Hibernate proxies, and entity graphs.

**Q6. Why is the cacheable method in a separate service bean?**

Spring's annotation-driven caching normally uses proxies. A call through `this` within the same bean can bypass the proxy and prevent the caching advice from running.

**Q7. Why must workspace authorization happen before cache access?**

Cached data must not bypass tenant isolation. The application must establish that the authenticated user can access the requested workspace before returning its data.

**Q8. What is the drawback of `allEntries = true`?**

It evicts every entry in the named cache, including entries for unrelated workspaces. This can create unnecessary cache misses.

**Q9. Does a passing Redis string test prove that DTO-list caching works?**

No. It verifies basic string storage and retrieval. The DTO list's serialization, deserialization, cache hits, and eviction require dedicated tests.

**Q10. What should be considered before using Redis caching in production?**

Serialization, cache invalidation, TTL, memory usage, concurrent misses, Redis outages, tenant isolation, monitoring, and integration testing.

## 10. Key Takeaway

Roost now has Redis configuration, a workspace-keyed cache service, DTO-based cached results, and cache eviction after monitor mutations. Authorization remains before cache access, and the full test suite passes.

The remaining verification gap is explicit: end-to-end DTO-list caching and eviction against Redis have not yet been proven. Caching is an optimization; correctness, tenant isolation, and reliable invalidation remain essential.

### Git Commit

```bash
git add .
git commit -m "feat: add Redis-backed workspace monitor caching"
```