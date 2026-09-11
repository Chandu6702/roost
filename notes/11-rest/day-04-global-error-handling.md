# Global Error Handling with `@RestControllerAdvice`

**Week 11 · Day 4** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

Global exception handling centralizes how errors from REST controllers are converted into HTTP responses. Spring's `@RestControllerAdvice` allows one class to handle exceptions thrown across controllers instead of repeating `try/catch` blocks in every endpoint.

For Roost, `GlobalExceptionHandler` handles validation failures, missing monitors, and unexpected server errors. It uses Spring's `ProblemDetail` to provide a consistent error response based on the RFC 7807 Problem Details format.

This keeps controllers focused on HTTP request handling while the global handler owns the API's error-response contract.

## 2. Mental Model

Think of exceptions as flowing into one central error-handling layer:

```text
Client
  ↓
Controller
  ↓
Service
  ↓
Exception
  ↓
@RestControllerAdvice
  ↓
ProblemDetail
  ↓
HTTP Error Response
```

Roost currently handles three main cases:

```text
MethodArgumentNotValidException
        ↓
       400
   Validation Failed

MonitorNotFoundException
        ↓
       404
  Monitor Not Found

Unexpected Exception
        ↓
       500
Internal Server Error
```

The important separation is:

```text
Service
  → detects/throws the problem

Exception
  → represents the problem

GlobalExceptionHandler
  → converts the problem into an HTTP response
```

## 3. My Roost Code

`src/main/java/com/chandu/roost/monitor/MonitorNotFoundException.java`

A custom runtime exception represents a missing monitor:

```java
public class MonitorNotFoundException extends RuntimeException {

    public MonitorNotFoundException(Long id) {
        super("Monitor with id " + id + " was not found");
    }
}
```

`src/main/java/com/chandu/roost/monitor/MonitorService.java`

The service now throws the exception instead of returning `null` when a monitor doesn't exist:

```java
Monitor monitor = monitors.get(id);

if (monitor == null) {
    throw new MonitorNotFoundException(id);
}

return monitor;
```

The same approach is used when updating or deleting a monitor that doesn't exist.

`src/main/java/com/chandu/roost/error/GlobalExceptionHandler.java`

The class is annotated with `@RestControllerAdvice` and handles the three main exception categories:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(...) {
        // return 400 ProblemDetail with fieldErrors
    }

    @ExceptionHandler(MonitorNotFoundException.class)
    public ProblemDetail handleMonitorNotFoundException(...) {
        // return 404 ProblemDetail
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(...) {
        // return generic 500 ProblemDetail
    }
}
```

The validation handler adds field-level errors such as:

```text
name → must not be blank
url → must be a valid URL
checkIntervalSeconds → must be greater than or equal to 30
```

The resulting error structure uses `ProblemDetail` with additional Roost properties:

```text
type
title
status
detail
timestamp
path
fieldErrors   ← validation errors only
```

Day 4 testing confirmed:

- Invalid monitor request → **400** with structured validation errors.
- Missing monitor → **404** with a structured `ProblemDetail`.
- Unexpected errors → **500** with a generic message rather than internal details.

## 4. Interview Points

- **What is `@RestControllerAdvice`?**
  - A global exception-handling mechanism for REST controllers.
  - It allows exception handling to be centralized instead of repeated in every controller.

- **Why use global exception handling?**
  - DRY code.
  - Consistent API error responses.
  - Keeps controllers clean.
  - Prevents accidental exposure of internal exception details.

- **What is `ProblemDetail`?**
  - Spring's representation of the RFC 7807 Problem Details format.
  - Provides standard error fields such as `type`, `title`, `status`, `detail`, and `instance`.
  - Custom properties can also be added.

- **Which handler wins if both controller-local and global handlers match?**
  - A controller-local `@ExceptionHandler` takes precedence.
  - The global advice acts as the broader fallback.

- **How should 4xx and 5xx errors be logged?**
  - Predictable 4xx errors generally don't need full stack traces.
  - Unexpected 5xx errors should be logged at `ERROR` with the full exception/stack trace.

- **Why shouldn't a 500 response expose the exception details?**
  - Internal implementation details, stack traces, SQL errors, and file paths can expose sensitive information and make the API less secure.

- **How can different failures share the same error format?**
  - Route each exception through `@RestControllerAdvice` and have each handler construct the same `ProblemDetail` structure.

## 5. Common Mistakes

- **Putting `try/catch` around every controller method**
  - This creates repetitive code and inconsistent error responses.
  - Centralize exception-to-response mapping in `@RestControllerAdvice`.

- **Returning `null` when a resource doesn't exist**
  - A missing resource should be represented explicitly by `MonitorNotFoundException`.

- **Putting HTTP logic inside the service**
  - The service should throw/represent the application problem.
  - The global handler decides the HTTP response.

- **Returning exception details to the client for a 500**
  - Never expose stack traces, SQL errors, or internal implementation details.

- **Using different error formats for different exceptions**
  - Clients should have a predictable error contract.
  - Validation, business, and unexpected errors should use a consistent `ProblemDetail` structure.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

```text
Exception occurs
      ↓
@RestControllerAdvice
      ↓
Find matching @ExceptionHandler
      ↓
Create ProblemDetail
      ↓
Return HTTP error response
```

Roost's current mapping:

```text
Validation failure          → 400
MonitorNotFoundException    → 404
Unexpected Exception        → 500
```

**Q&A**

**Q1. What is the purpose of `@RestControllerAdvice`?**
It provides centralized exception handling for REST controllers.

**Q2. Why is global exception handling better than `try/catch` in every controller?**
It avoids repetition, keeps controllers clean, and provides a consistent error contract.

**Q3. What is `ProblemDetail`?**
Spring's representation of the RFC 7807 Problem Details format for standardized HTTP API errors.

**Q4. What happens when a controller-local and global `@ExceptionHandler` can both handle an exception?**
The controller-local handler takes precedence; the global advice acts as the fallback.

**Q5. Why does `MonitorService` throw `MonitorNotFoundException` instead of returning `null`?**
It explicitly represents the missing-resource condition and allows the global handler to convert it into a 404 response.

**Q6. Should a 500 response contain the actual exception message?**
Generally no. The client should receive a generic message while the server logs the actual exception.

**Q7. How should unexpected 5xx errors be logged?**
At `ERROR` level with the full exception and stack trace.

**Q8. How can validation errors and business-rule errors use the same API format?**
Handle both through `@RestControllerAdvice` and construct `ProblemDetail` responses with the same overall schema.

**Q9. What is the role of `fieldErrors`?**
It provides field-specific validation messages and is included for validation failures.

**Q10. What are the standard Problem Details fields?**
`type`, `title`, `status`, `detail`, and `instance`.
