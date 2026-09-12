# Week 11 — REST API Interview Q&A

## 1. What is the difference between `@Controller` and `@RestController`?

`@Controller` is typically used for MVC controllers that return views. `@RestController` is effectively `@Controller + @ResponseBody`, so return values are written directly to the HTTP response body, usually as JSON.

---

## 2. What is the difference between `@GetMapping`, `@PostMapping`, `@PutMapping`, and `@DeleteMapping`?

They are composed annotations for mapping controller methods to specific HTTP methods:

- `@GetMapping` → retrieve resources
- `@PostMapping` → create resources
- `@PutMapping` → replace/update a resource
- `@DeleteMapping` → delete a resource

They are preferred over repeatedly writing `@RequestMapping(method = ...)` because they are shorter and more expressive.

---

## 3. What is the difference between `@PathVariable`, `@RequestParam`, and `@RequestBody`?

- `@PathVariable` → value embedded in the URL path
  `/monitors/42`
- `@RequestParam` → query-string parameter
  `/monitors?page=0&size=10`
- `@RequestBody` → data sent in the HTTP request body, usually JSON

Example:

```text
GET /monitors/42?page=0
```

Here `42` is a path variable and `0` is a request parameter.

---

## 4. What is the purpose of `@RequestMapping` at class level?

It defines a common base path for all endpoints in the controller.

```java
@RestController
@RequestMapping("/monitors")
public class MonitorController {
```

Then:

```java
@GetMapping
```

maps to:

```text
GET /monitors
```

and:

```java
@GetMapping("/{id}")
```

maps to:

```text
GET /monitors/{id}
```

It does **not** mean that all methods use the same HTTP method.

---

## 5. Why should an API use DTOs instead of exposing domain entities directly?

DTOs create a boundary between the internal domain model and the external API contract.

They prevent:

- exposing internal fields
- accidental sensitive-data exposure
- over-posting/mass-assignment problems
- coupling the API contract to the database/entity structure
- unwanted serialization of relationships

They also allow the database/domain model to evolve without unnecessarily breaking the public API.

---

## 6. Why are Java records useful for DTOs?

Records provide a concise way to define immutable data carriers.

They automatically provide:

- final fields
- canonical constructor
- accessor methods
- `equals()`
- `hashCode()`
- `toString()`

For simple request/response DTOs, this removes a large amount of boilerplate.

---

## 7. How does Jackson deserialize a Java record?

Jackson uses the record's canonical constructor and its component names to construct the object from JSON.

For example:

```java
public record MonitorRequest(
    String name,
    String url
) {}
```

JSON:

```json
{
  "name": "Google",
  "url": "https://google.com"
}
```

Jackson maps the JSON properties to the record components and invokes the canonical constructor.

---

## 8. What is the difference between `@JsonIgnore` and `@JsonProperty(access = READ_ONLY)`?

`@JsonIgnore` prevents a property from participating in JSON serialization/deserialization.

`READ_ONLY` means:

- included when sending the object **to the client**
- ignored when receiving the object **from the client**

This is useful for server-generated fields such as:

```text
id
createdAt
updatedAt
```

A client can receive the ID but should not be allowed to provide or modify it.

---

## 9. Where should request validation normally happen?

At the API boundary, on the request DTO.

For example:

```java
public record MonitorRequest(
    @NotBlank String name,
    @NotBlank @URL String url
) {}
```

and:

```java
public ResponseEntity<?> create(
        @Valid @RequestBody MonitorRequest request)
```

This prevents invalid external input from entering the application layer.

Domain/business invariants can still be enforced separately inside the domain/service layer.

---

## 10. What happens internally when `@Valid @RequestBody` fails?

Spring attempts to deserialize the JSON into the request DTO and then performs Bean Validation.

If validation fails, Spring raises:

```text
MethodArgumentNotValidException
```

The controller method itself is not executed.

Our `@RestControllerAdvice` catches that exception and converts it into our consistent `ProblemDetail` response.

---

## 11. Why use `@RestControllerAdvice`?

It provides centralized exception handling across controllers.

Instead of writing:

```java
try/catch
```

or individual exception handlers in every controller, we define the handling once.

Benefits:

- consistent error responses
- less duplicated code
- centralized logging/error policy
- cleaner controllers
- easier maintenance

---

## 12. What is `ProblemDetail` and why is it useful?

`ProblemDetail` is Spring's support for the standardized **RFC 7807 Problem Details for HTTP APIs** format.

It gives APIs a consistent structure for errors such as:

```json
{
  "type": "...",
  "title": "Monitor Not Found",
  "status": 404,
  "detail": "Monitor with id 999 was not found"
}
```

We can add application-specific properties such as:

```text
timestamp
path
fieldErrors
```

This gives clients a predictable error contract.

---

## 13. Why should a service throw a custom exception instead of returning `null` for a missing resource?

A custom exception makes the business failure explicit.

For example:

```java
throw new MonitorNotFoundException(id);
```

The global exception handler can then consistently translate it into:

```text
HTTP 404 Not Found
```

Returning `null` makes it easier for the error to propagate ambiguously and potentially result in an incorrect `200 OK` or a `NullPointerException`.

---

## 14. When should you return `ResponseEntity` instead of simply returning an object?

Use `ResponseEntity` when you need control over the HTTP response, such as:

- status code
- headers
- response body

For example, a successful creation should normally return:

```text
201 Created
```

while a simple successful GET can often just return an object and let Spring produce:

```text
200 OK
```

---

## 15. What HTTP status codes should a REST API commonly use for CRUD operations?

Typical choices:

| Operation                  | Status                       |
| -------------------------- | ---------------------------- |
| Successful GET             | `200 OK`                     |
| Successful POST/create     | `201 Created`                |
| Successful PUT             | `200 OK` or `204 No Content` |
| Successful DELETE          | `204 No Content`             |
| Resource not found         | `404 Not Found`              |
| Invalid request/validation | `400 Bad Request`            |
| Unexpected server failure  | `500 Internal Server Error`  |

For a `201 Created`, a production API may also return a `Location` header pointing to the newly created resource.

---

## 16. What is OpenAPI and how does Swagger UI relate to it?

**OpenAPI** is a machine-readable specification describing an HTTP API.

It can describe:

- endpoints
- HTTP methods
- parameters
- request bodies
- response schemas
- status codes
- authentication

**Swagger UI** is a web interface that renders that OpenAPI specification and lets developers interact with the API.

In Roost, this means a reviewer can open Swagger UI, understand the API contract, and execute requests without first reading the Java code or manually constructing `curl` commands.

---

## 17. Why is API documentation important in a real production project?

Good API documentation acts as a contract between the backend and its consumers.

It helps:

- frontend developers understand endpoints
- QA test APIs
- external consumers integrate with the service
- generate clients/documentation
- reduce misunderstandings
- onboard new developers faster

In a project such as Roost, Swagger UI also provides an immediate way to demonstrate that the REST layer is usable.

---

## 18. How does manual pagination work?

Suppose:

```text
page = 1
size = 10
```

Since pages are zero-based, page `1` means the **second page**.

The starting index is:

```java
start = page * size;
```

So:

```text
start = 1 × 10 = 10
```

The ending index is:

```java
end = Math.min(start + size, totalElements);
```

Then:

```java
list.subList(start, end)
```

returns that page's content.

---

## 19. Why should sorting happen before pagination?

Because pagination should operate on the **ordered dataset**.

Correct:

```text
all records
    ↓
sort
    ↓
paginate
    ↓
return page
```

If you paginate first and sort afterward, each page can contain the wrong records relative to the requested global ordering.

This becomes especially important when moving pagination to the database.

---

## 20. Why is fetching 100,000 database rows and paginating them in Java a bad design?

Because the application unnecessarily:

- transfers large amounts of data from the database
- consumes JVM memory
- performs sorting/pagination work in the application
- increases response time
- reduces scalability

With a real database, pagination should normally happen at the database level using mechanisms such as:

```text
ORDER BY
LIMIT
OFFSET
```

or Spring Data's `Pageable`.

The database should return only the rows needed for the requested page.

---

## 21. What is the difference between manual pagination and Spring Data `Page<>`?

Our Week 11 implementation performs pagination manually:

```text
List → sort → calculate indexes → subList
```

With Spring Data JPA, the database performs the pagination and Spring can return a `Page<T>` containing:

- page content
- page number
- page size
- total elements
- total pages
- pagination metadata

Manual pagination is useful for understanding the mechanics. `Pageable`/`Page<>` becomes the appropriate production approach once Roost is backed by JPA in Week 12.

---

## 22. How would you explain the complete request flow in Roost?

A strong interview answer:

```text
HTTP request
    ↓
Controller
    ↓
Request DTO + Jackson deserialization
    ↓
Bean Validation
    ↓
Service
    ↓
Domain model
    ↓
Response DTO
    ↓
HTTP response
```

If something fails:

```text
Exception
    ↓
@RestControllerAdvice
    ↓
ProblemDetail
    ↓
HTTP error response
```

This is the core mental model for the REST layer we built in Week 11.

---


**Week 11 takeaway:**
A production REST API is not just a collection of controller endpoints. It needs a clear API contract, DTO boundary, input validation, consistent error handling, documentation, and scalable pagination.
