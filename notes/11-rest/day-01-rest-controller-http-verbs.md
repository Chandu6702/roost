# REST Controller & HTTP Verbs

**Week 11 · Day 1** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

A REST controller exposes application functionality through HTTP endpoints. In Spring Boot, `@RestController` tells Spring that a class handles HTTP requests and that its return values should be written directly to the HTTP response body. Today we built the first REST CRUD layer for Roost using five endpoints for a `Monitor` resource. The data is temporarily stored in an in-memory `Map<Long, Monitor>`; database persistence comes in W12.

## 2. Mental Model

The basic REST flow:

```text
HTTP Request
     ↓
MonitorController
     ↓
MonitorService
     ↓
Map<Long, Monitor>
     ↓
HTTP Response
```

Responsibilities:

```text
Monitor
    → Represents one monitor/data

MonitorController
    → Handles HTTP requests, paths, request bodies
    → Maps HTTP operations to service methods

MonitorService
    → Contains application logic
    → Manages the in-memory collection of monitors

Map<Long, Monitor>
    → Temporary storage for W11
```

### CRUD → HTTP verbs

```text
Create → POST
Read   → GET
Update → PUT
Delete → DELETE
```

### Five endpoints

| HTTP   | Endpoint         | Purpose          | Success          |
| ------ | ---------------- | ---------------- | ---------------- |
| GET    | `/monitors`      | Get all monitors | `200 OK`         |
| GET    | `/monitors/{id}` | Get one monitor  | `200 OK`         |
| POST   | `/monitors`      | Create monitor   | `201 Created`    |
| PUT    | `/monitors/{id}` | Update monitor   | `200 OK`         |
| DELETE | `/monitors/{id}` | Delete monitor   | `204 No Content` |

For a missing monitor:

```text
404 Not Found
```

## 3. My Roost Code

### Files

```text
src/main/java/com/chandu/roost/monitor/
├── Monitor.java
├── MonitorController.java
└── MonitorService.java
```

`Monitor` is a Java record because it is currently a simple immutable data carrier:

```java
public record Monitor(
        Long id,
        String name,
        String url,
        String status
) {
}
```

The service owns the in-memory collection:

```java
@Service
public class MonitorService {

    private final Map<Long, Monitor> monitors = new HashMap<>();
    private long nextId = 1;

    // CRUD operations use this map as temporary storage.
}
```

The controller uses a class-level base mapping:

```java
@RestController
@RequestMapping("/monitors")
public class MonitorController {

    @GetMapping
    public ResponseEntity<List<Monitor>> getMonitors() {
        return monitorService.getMonitors();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Monitor> getMonitor(@PathVariable Long id) {
        return monitorService.getMonitor(id);
    }
}
```

The class-level mapping:

```text
/monitors
```

combines with the method-level mapping:

```text
/{id}
```

to produce:

```text
GET /monitors/{id}
```

### Request data annotations

```text
@PathVariable
    /monitors/{id}
              ↑
           URL path

@RequestParam
    /monitors?status=UP
              ↑
         query parameter

@RequestBody
    POST /monitors
    {
      "name": "Google",
      "url": "https://google.com"
    }
              ↑
         request body
```

## 4. Interview Points

### `@RequestMapping` vs `@GetMapping`

`@GetMapping` is a composed shortcut for:

```java
@RequestMapping(method = RequestMethod.GET)
```

Prefer `@GetMapping` when defining a GET endpoint because it is shorter and makes the HTTP method immediately clear.

### `@PathVariable` vs `@RequestParam` vs `@RequestBody`

* `@PathVariable` → value from the URL path: `/monitors/10`
* `@RequestParam` → value from the query string: `/monitors?status=UP`
* `@RequestBody` → data from the HTTP request body, usually JSON

### `ResponseEntity`

Use `ResponseEntity` when the controller needs explicit control over:

* HTTP status
* response headers
* response body

For example, a successful creation can return:

```text
201 Created
Location: /monitors/1
```

A plain object is sufficient when the default response behavior, typically `200 OK`, is enough.

### HTTP status codes

```text
200 OK
    Successful request

201 Created
    Resource successfully created

204 No Content
    Successful operation with no response body

404 Not Found
    Requested resource does not exist
```

### Class-level vs method-level mapping

Class-level `@RequestMapping` defines a common/base path for the controller.

Method-level mappings define the specific endpoint path and HTTP method.

Example:

```java
@RequestMapping("/monitors")  // base path

@GetMapping("/{id}")          // method mapping
```

becomes:

```text
GET /monitors/{id}
```

## 5. Common Mistakes

* Don't put the in-memory `Map` inside `Monitor`.

  * `Monitor` represents one monitor.
  * `MonitorService` manages the collection.

* Don't inject `Monitor` into `MonitorService`.

  * `Monitor` is a data object, not a Spring service dependency.

* Don't make the `Map` static for this exercise.

  * The `MonitorService` bean owns the in-memory state.

* Remember that class-level `@RequestMapping` is primarily for the common/base path; it does not mean the entire controller uses one HTTP method.

* Successful DELETE should normally return `204 No Content` when there is no response body.

* A GET for a nonexistent resource should return `404 Not Found`, not `200 OK` with `null`.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

A Spring REST controller exposes HTTP endpoints. For Roost, the controller handles `/monitors` requests and delegates to `MonitorService`. `GET` reads, `POST` creates, `PUT` updates, and `DELETE` removes. `@PathVariable` reads URL path values, `@RequestParam` reads query parameters, and `@RequestBody` reads JSON request data. `ResponseEntity` is used when explicit control over status, headers, or body is required.

### Q&A

**Q1. What does `@RestController` do?**

It marks a class as a Spring REST controller. Its handler method return values are written directly to the HTTP response body.

**Q2. What is the difference between `@RequestMapping` and `@GetMapping`?**

`@GetMapping` is a shortcut for `@RequestMapping(method = RequestMethod.GET)`.

**Q3. What is `@PathVariable` used for?**

To extract a value from the URL path.

```text
/monitors/10
```

Here `10` can be captured with `@PathVariable`.

**Q4. What is `@RequestParam` used for?**

To extract query-string parameters.

```text
/monitors?status=UP
```

Here `status` can be captured with `@RequestParam`.

**Q5. What is `@RequestBody` used for?**

To deserialize the HTTP request body, commonly JSON, into a Java object.

**Q6. Why use `ResponseEntity`?**

When we need explicit control over the HTTP status, headers, and/or response body.

**Q7. What status should a successful POST create return?**

`201 Created`.

**Q8. What status should a successful DELETE with no body return?**

`204 No Content`.

**Q9. What status should a GET return when the requested monitor doesn't exist?**

`404 Not Found`.

**Q10. Why use class-level `@RequestMapping("/monitors")`?**

To define a common base path for all endpoints in the controller. Method-level mappings are then combined with that base path.

**Q11. Why is `MonitorService` responsible for the `Map` instead of `Monitor`?**

Because `Monitor` represents one resource, while `MonitorService` manages the collection and application logic.

**Q12. Why is `Monitor` a record in this implementation?**

Because it is currently a simple immutable data carrier with no significant behavior. A record provides the constructor, accessors, `equals`, `hashCode`, and `toString` automatically.

## Day 1 Result

```text
REST Controller              ✅
HTTP verbs                   ✅
@RequestMapping              ✅
@GetMapping                  ✅
@PathVariable                ✅
@RequestParam                ✅
@RequestBody                 ✅
ResponseEntity               ✅
HTTP status codes            ✅
In-memory CRUD               ✅
All 5 endpoints tested       ✅
```

**Genuine correction from today:**

Class-level `@RequestMapping` defines a common/base path; it does not make the whole controller use the same HTTP method.

**Testing note:**

The initial POST failure was caused by using Windows CMD line-continuation syntax in Git Bash. The corrected Git Bash command worked. This was a shell-command issue, not a Spring REST implementation mistake.
