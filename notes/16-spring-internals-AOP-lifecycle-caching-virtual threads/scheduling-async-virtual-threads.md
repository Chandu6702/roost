# Scheduled Probes with Async Execution and Virtual Threads

**Week 16 · Day 5** · [DAILY-PLAN](../../DAILY-PLAN.md)

## Definition & Why

Spring scheduling triggers work at defined intervals. Asynchronous execution lets a scheduled method dispatch work without waiting for each task to finish. Virtual threads make blocking I/O workloads easier to scale without dedicating a platform thread to every task.

In Roost, I connected the existing probe workflow to scheduled execution. The scheduler discovers due probe schedules, claims each schedule before dispatch, and executes each probe asynchronously. Probe results are persisted to PostgreSQL.

## Mental Model

```text
@EnableScheduling
        ↓
ProbeScheduler discovers due schedules
        ↓
Atomically claim each due schedule
        ↓
ObjectProvider creates a prototype ProbeTask
        ↓
@Async dispatches execution
        ↓
Virtual thread runs the probe
        ↓
ProbeRunner executes
        ↓
ProbeResult is saved
```

- `@EnableScheduling` enables scheduled-method execution.
- `@Scheduled` runs schedule discovery at a fixed delay.
- `@EnableAsync` enables asynchronous method execution.
- `@Async` dispatches the probe task asynchronously.
- Prototype scope creates a new `ProbeTask` instance for each provider request.
- `spring.threads.virtual.enabled: true` enables Spring Boot's virtual-thread support for its configured executor.
- An atomic conditional update claims a due schedule and advances its next-run time.

## My Roost Code

**1. Application configuration**

Path: `src/main/java/com/chandu/roost/RoostApplication.java`

- Uses `@EnableScheduling` and `@EnableAsync`.
- Retains the existing `@SpringBootApplication`, configuration-properties, and caching setup.

Path: `src/main/resources/application.yml`

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

This setting is under the existing `spring` section. Runtime logs confirmed `Thread.currentThread().isVirtual()` returned `true`.

**2. Scheduler**

Path: `src/main/java/com/chandu/roost/service/ProbeScheduler.java`

- Uses `@Scheduled(fixedDelay = 10000)` to discover due schedules.
- Reads schedules whose `nextProbeAt` is due.
- Calculates the next run using the monitor's configured interval.
- Dispatches only when the atomic claim succeeds.
- Uses `ObjectProvider<ProbeTask>` to obtain a prototype task.

**3. Schedule repository**

Path: `src/main/java/com/chandu/roost/repository/ProbeScheduleRepository.java`

- `findByNextProbeAtLessThanEqual(...)` discovers due schedules.
- `claimDueSchedule(...)` conditionally advances a schedule only if it is still due.

The conditional update helps prevent multiple scheduler attempts from dispatching the same due schedule.

**4. Asynchronous worker**

Path: `src/main/java/com/chandu/roost/service/ProbeTask.java`

- Uses prototype scope.
- Executes through `@Async`.
- Loads the schedule and associated monitor.
- Calls `ProbeRunner.run(...)`.
- Records status, latency, and any error in `ProbeResult`.
- Uses `@Transactional` for the worker's database operations.

**5. Probe execution and persistence**

Relevant paths:

- `src/main/java/com/chandu/roost/probe/ProbeRunner.java`
- `src/main/java/com/chandu/roost/entity/ProbeResult.java`
- `src/main/java/com/chandu/roost/repository/ProbeResultRepository.java`

The existing `LoggingAspect` continues to measure `ProbeRunner.run(...)`.

**Verification completed**

- Application started successfully; Flyway reported the schema was up to date.
- Three due schedules were dispatched.
- Worker logs showed `virtual=true` for all three executions.
- SQL logs showed inserts into `probe_results`.
- Completion logs showed successful execution for monitors 25, 26, and 27.
- The scheduler used conditional updates to advance claimed schedules.

**Scope limitation:** `HttpProbeClient` still returns a stub response. The current workflow verifies scheduling, asynchronous execution, persistence, and virtual-thread usage—not real external HTTP health checks.

## Interview Points

1. **What does `@Scheduled` do?**

   It schedules a method to execute according to a timing configuration, such as a fixed delay.

2. **What is the difference between fixed rate and fixed delay?**

   Fixed rate targets a regular interval between invocation start times. Fixed delay waits for the previous invocation to finish before measuring the delay to the next invocation.

3. **What does `@EnableScheduling` do?**

   It enables Spring's scheduled-task processing.

4. **What does `@Async` do?**

   It dispatches a method invocation to an asynchronous executor rather than requiring the caller to wait for the method to finish.

5. **Why use virtual threads for probe execution?**

   Probe workloads can involve blocking I/O. Virtual threads reduce the cost of managing large numbers of concurrent tasks compared with dedicating a platform thread to each task.

6. **How did I verify virtual-thread execution?**

   By logging `Thread.currentThread().isVirtual()` from inside `ProbeTask.execute(...)` and observing `virtual=true`.

7. **Why use `ObjectProvider<ProbeTask>`?**

   It obtains a fresh prototype-scoped task instance when requested instead of injecting one instance directly.

8. **Why claim a schedule before dispatching its task?**

   The conditional database update reduces duplicate dispatches when a schedule is already claimed or is no longer due.

9. **Does `@Transactional` on the scheduler automatically cover the async worker?**

   No. The asynchronous worker runs separately and needs its own transaction boundary for its database work.

10. **What is a limitation of claiming before dispatch?**

    If the application crashes after advancing the schedule but before the task completes, that probe may be missed until a later scheduled run. Durable retry or lease recovery would require additional design.

11. **Do these logs prove real URL monitoring works?**

    No. They verify the execution pipeline. The current `HttpProbeClient` is a stub and must be replaced or implemented for real HTTP checks.

## Common Mistakes

- Assuming `@Async` works without enabling asynchronous execution.
- Assuming `@Scheduled` works without enabling scheduling.
- Treating thread names such as `task-1` as proof of virtual threads instead of checking `isVirtual()`.
- Dispatching the same due schedule repeatedly without advancing or claiming it.
- Assuming a transaction on the scheduler propagates to a separate asynchronous invocation.
- Assuming successful stub responses prove real network health checks.
- Ignoring the possibility of a lost run between claiming a schedule and completing its task.

## Quick Revision

```text
@EnableScheduling
    → Enables scheduled methods

@Scheduled
    → Discovers due schedules periodically

Atomic schedule claim
    → Prevents duplicate claims

ObjectProvider + prototype scope
    → Obtains a fresh task instance

@EnableAsync + @Async
    → Dispatches work asynchronously

spring.threads.virtual.enabled
    → Enables Boot virtual-thread support

Thread.currentThread().isVirtual()
    → Verifies the executing thread type

ProbeResultRepository
    → Persists probe results
```

## Q&A

**Q1. Which component discovers due schedules?**

`ProbeScheduler`.

**Q2. Which component executes an individual probe?**

`ProbeTask`, which calls `ProbeRunner`.

**Q3. Why is schedule claiming conditional?**

To reduce duplicate dispatch when the schedule is no longer due.

**Q4. How was virtual-thread support verified?**

The worker logs reported `virtual=true`.

**Q5. Where are probe results stored?**

In PostgreSQL through `ProbeResultRepository`.

**Q6. Does the scheduler wait for every probe to finish?**

No. It dispatches each task asynchronously.

**Q7. What remains incomplete for production monitoring?**

Real HTTP probing, plus any required timeout handling, retry/recovery, and failure-management behavior.

## Key Takeaway

Roost now connects **scheduled discovery → conditional claiming → asynchronous virtual-thread execution → result persistence**. Runtime logs confirmed the three dispatched probes executed on virtual threads and saved their results.

This completes the Week 16 Day 5 implementation and runtime verification. Real external HTTP monitoring remains separate work because the current HTTP client is still a stub.

## Git Commit

`feat: execute scheduled probes asynchronously with virtual threads`