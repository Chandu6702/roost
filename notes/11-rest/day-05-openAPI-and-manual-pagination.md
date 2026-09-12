# OpenAPI & Manual Pagination

**Week 11 · Day 5** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

**OpenAPI/Swagger** provides a machine-readable description and interactive documentation for the REST API. **Pagination** limits how many resources an endpoint returns at once. In Roost, OpenAPI makes the `/monitors` API easy to understand and test through Swagger UI, while manual pagination prepares us for database-level pagination with Spring Data JPA in Week 12.

## 2. Mental Model

Think of two separate concerns:

```text
REST API
    ↓
OpenAPI specification
    ↓
Swagger UI
    ↓
Developer can understand + execute endpoints
```

For pagination:

```text
All monitors
    ↓
Sort
    ↓
Calculate page boundaries
    ↓
Return requested page
```

Example:

```text
page = 1
size = 2

start = page × size
      = 1 × 2
      = 2
```

So page `1` contains the third and fourth items because pages are **zero-based**.

Production flow in Week 12:

```text
Database
    ↓
ORDER BY + LIMIT/OFFSET
    ↓
Only requested rows
    ↓
Page<T>
```

## 3. My Roost Code

### OpenAPI dependency

`pom.xml`

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.1.1</version>
</dependency>
```

### Endpoint documentation

`src/main/java/com/chandu/roost/monitor/MonitorController.java`

```java
@GetMapping
@Operation(
    summary = "Get all monitors",
    description = "Retrieve a list of all monitors"
)
public ResponseEntity<MonitorPageResponse> getMonitors(...) {
```

### Pagination response

`src/main/java/com/chandu/roost/monitor/MonitorPageResponse.java`

```java
public record MonitorPageResponse(
        List<MonitorResponse> content,
        int page,
        int size,
        long totalElements
) {}
```

### Manual pagination

`src/main/java/com/chandu/roost/monitor/MonitorService.java`

```java
int start = page * size;
int end = Math.min(start + size, allMonitors.size());

if (start >= allMonitors.size()) {
    return List.of();
}

return allMonitors.subList(start, end);
```

### Sorting

The service sorts monitors before applying pagination:

```text
all monitors → sort → paginate → response
```

### Verification

The endpoint successfully returned:

```json
{
  "content": [...],
  "page": 0,
  "size": 2,
  "totalElements": 5
}
```

Swagger UI is available at:

```text
/swagger-ui.html
```

OpenAPI JSON is available at:

```text
/v3/api-docs
```

## 4. Interview Points

- **OpenAPI** describes an HTTP API; **Swagger UI** provides an interactive interface for that specification.
- Swagger documentation lets developers/reviewers understand and execute the API without reading implementation code or manually constructing `curl` requests.
- Pagination uses zero-based `page` numbering.
- Manual pagination uses `start = page × size` and `end = min(start + size, totalElements)`.
- **Sort before pagination** so the requested page represents the correct global ordering.
- Manual `subList()` pagination is suitable for learning, but production database-backed pagination should happen at the database level using `Pageable`/`Page<T>`.

## 5. Common Mistakes

- Treating `page=1` as the first page instead of the second page.
- Paginating before sorting.
- Fetching the entire database table into JVM memory for large datasets.
- Returning only the page content without useful pagination metadata.
- Forgetting that the controller return type must match the actual response wrapper (`MonitorPageResponse`).
- Using a springdoc version intended for the wrong Spring Boot major version.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

OpenAPI documents the REST contract, and Swagger UI makes that contract interactive. Pagination prevents an endpoint from returning an unnecessarily large dataset. In our manual implementation, we sort the in-memory monitors first, calculate `start` and `end`, and return a `MonitorPageResponse` containing the content and pagination metadata. With JPA in Week 12, pagination will move to the database using `Pageable`.

**Q&A**

**Q: Why do we sort before paginating?**
A: To ensure each page represents the correct position in the globally sorted dataset.

**Q: What does `page=1&size=10` mean?**
A: The second page containing up to 10 items.

**Q: Why isn't manual `subList()` pagination suitable for large databases?**
A: It requires loading the full dataset into application memory first. Production systems should let the database perform sorting and pagination.

**Q: What is Swagger UI's practical value?**
A: Developers and reviewers can inspect and execute API endpoints directly without reading the source code or writing `curl` commands.

**Q: What changes in Week 12?**
A: Pagination moves from the in-memory `Map` to database-level pagination using Spring Data JPA's `Pageable`/`Page<T>`.

**Key takeaway:**
**Document the API clearly, validate the contract, and paginate at the layer that can scale.**
