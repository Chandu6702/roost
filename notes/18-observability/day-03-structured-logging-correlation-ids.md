# W18 D3 — Structured logging + correlation IDs

## Definition & Why
**Structured logging** emits each log line as JSON (`{"level":..,"message":..,"requestId":..}`) instead of free text, so aggregators (ELK/Loki/Datadog) can index and query fields. A **correlation id** (request id) is one value stamped on every log line of a single request, so you can reconstruct one request's whole story across threads and services with a single filter.

## Mental Model
Plain logs are a pile of loose receipts; you `grep` and hope. JSON logs are a spreadsheet — filter by `requestId`, `level`, `logger`. The **MDC** (Mapped Diagnostic Context) is a per-thread sticky note the logging framework automatically stamps onto every line that thread writes. Set it once at the edge (the filter), and everything downstream inherits it — as long as you clean it off when the thread is recycled, and carry it across async hops.

## My Roost Code (today)
- `RequestIdFilter` (`HIGHEST_PRECEDENCE`) — reuses an inbound `X-Request-Id` or mints a UUID, `MDC.put("requestId", …)`, echoes it on the response, and `MDC.remove` in `finally`.
- `logback-spring.xml` — `prod` profile logs JSON via `LogstashEncoder` (with `requestId` as a key); other profiles use a readable pattern that still shows `[%X{requestId}]`.
- `MdcTaskDecorator` — a `TaskDecorator` that snapshots the MDC at submit time and installs it on the async worker thread (restoring the worker's prior MDC after), wired into the auto-configured async executor via `SimpleAsyncTaskExecutorCustomizer`. Without it, `@Async` probe logs would have an empty requestId.

### Try it
`curl -H 'X-Request-Id: test-123' http://localhost:8080/health` → every log line for that call carries `test-123`.

## Interview Points
- **Why JSON in prod**: machine-parseable → searchable/aggregatable. Plain text = grep only.
- **MDC lifecycle**: per-thread; set in a filter, *cleared in finally* or a recycled thread leaks the previous request's id.
- **Async pitfall**: a new thread starts with an empty MDC — copy it with a `TaskDecorator`.
- **Never log secrets/PII**: no raw request bodies, passwords, tokens — redact at the appender or don't log them.
- Log levels: DEBUG = flow detail, INFO = business events, WARN = recoverable oddities, ERROR = needs attention (with stack).

## Common Mistakes
- Forgetting `MDC.remove`/`clear` → correlation ids bleed between requests on pooled threads.
- Assuming MDC crosses `@Async`/executor boundaries automatically (it doesn't).
- Logging full request bodies → leaking credentials into the log store.
- Putting JSON logging only in code and not guarding dev readability.

## Quick Revision + Q&A
**Q: What is the MDC and when is it cleared?**
A: A per-thread key/value map the logging framework stamps on each line. Cleared at request end in the filter's `finally`, else it leaks to the next request on the same thread.

**Q: A probe runs on an async thread — how does its log keep the request id?**
A: A `TaskDecorator` copies the submitting thread's MDC onto the async thread around the task.

**Q: Why JSON logs over plain text?**
A: Aggregators index JSON fields for structured search and alerting; plain text only supports grep.
