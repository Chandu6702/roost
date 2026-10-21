# Week 16 — Spring Bean Internals: AOP, Lifecycle & Scopes

**Roadmap:** [DAILY-PLAN.md](../../DAILY-PLAN.md)

## 1. Definition & Why

Spring bean internals determine how Spring-managed objects are created, initialized, intercepted, reused, and destroyed.

This week's topics cover three related mechanisms:

- **Aspect-Oriented Programming (AOP):** Adds cross-cutting behavior, such as execution-time logging, around existing method calls.
- **Bean lifecycle:** Defines initialization and destruction callbacks for Spring-managed beans.
- **Bean scopes:** Determine how Spring creates and reuses bean instances.

These concepts matter because production applications need consistent logging, predictable object lifecycles, and correct handling of shared versus per-operation state.

### Why it matters in Roost

- **AOP:** Measure execution time without adding repetitive timing logic to every service method.
- **Lifecycle:** Initialize components after their dependencies are available and perform appropriate shutdown actions.
- **Scopes:** Avoid accidentally sharing mutable state when an operation requires its own object instance.
- **Maintainability:** Separate infrastructure concerns from business logic.

## 2. Mental Models

### AOP: A proxy around a method call

```text
Caller
  |
  v
Spring AOP proxy
  |
  v
LoggingAspect (@Around)
  |
  v
Target method
  |
  v
Return result / propagate exception
```

The aspect surrounds the method invocation. It can perform work before and after the target method executes.

In Roost, `@LogExecutionTime` identifies methods whose execution duration should be logged. `LoggingAspect` implements the timing behavior.

### Bean lifecycle

```text
Spring creates bean
        |
        v
Dependencies injected
        |
        v
@PostConstruct
        |
        v
Bean available for use
        |
        v
Application context shuts down
        |
        v
@PreDestroy
```

This is a simplified lifecycle model for an ordinary Spring-managed bean. Other lifecycle callbacks and post-processors can participate in the full Spring lifecycle.

### Bean scopes

Think of scope as answering this question:

**When Spring is asked for this bean, should it reuse an existing instance or create another one?**

- `singleton`: Reuses one instance per Spring application context.
- `prototype`: Creates a new instance for each request to the container.
- Web scopes: Associate instances with a request, session, application, or WebSocket session.

An important distinction: scope governs how Spring resolves instances. It does not determine how many times a method on an existing object can be called.

## 3. My Roost Code

### A. AOP execution-time logging

Relevant implementation:

- `@LogExecutionTime` — custom annotation identifying methods to measure.
- `LoggingAspect` — implements the `@Around` advice and records execution duration.
- `ProbeRunner.run(...)` — one instrumented method.
- `IncidentService.open(...)` — another instrumented method.

The aspect uses the `ProceedingJoinPoint` to invoke the target method through `proceed()` and measure the elapsed time.

Conceptually:

```java
long start = System.nanoTime();

try {
    return joinPoint.proceed();
} finally {
    long elapsed = System.nanoTime() - start;
    // Log elapsed execution time.
}
```

This is the core timing pattern; the actual implementation remains in `LoggingAspect`.

Using `finally` ensures that execution time can be recorded even when the target method throws an exception, provided the logging code itself executes successfully.

**Verification**

Roost produced a log similar to:

```text
Method IncidentService.open(..) executed in 35 ms
```

This confirmed that the aspect intercepted the instrumented method and logged its duration.

### B. Spring AOP proxy considerations

Spring AOP generally applies advice through a proxy around the target bean.

Two common proxy strategies are:

- **JDK dynamic proxies:** Proxy interfaces implemented by the target.
- **CGLIB proxies:** Create a subclass-based proxy when appropriate.

The proxy mechanism and configuration determine which strategy is used.

**Important limitation — self-invocation**

When a method on a bean calls another method on the same object using `this`, the call normally bypasses the Spring AOP proxy. Advice on the internally invoked method therefore does not run through the usual proxy-based interception path.

For example:

```text
External caller
      |
      v
Spring proxy
      |
      v
methodA()
      |
      v
this.methodB()
```

The call from `methodA()` to `methodB()` does not normally pass back through the proxy.

### C. Bean lifecycle: `ProbeScheduler`

**File:** `src/main/java/com/chandu/roost/service/ProbeScheduler.java`

The component uses `@PostConstruct` and `@PreDestroy`:

```java
@PostConstruct
public void initialize() {
    log.info("Probe scheduler ready");
}

@PreDestroy
public void shutdown() {
    log.info("Probe scheduler shutting down");
}
```

These methods demonstrate initialization and destruction callbacks.

**What each annotation does**

- `@PostConstruct`: Runs after dependency injection and before the bean is put into service.
- `@PreDestroy`: Runs when Spring is destroying the managed bean, such as during a graceful application-context shutdown.

**Verification**

Roost produced these messages:

```text
Probe scheduler ready
```

During a separate startup attempt that failed because port 8080 was already occupied, the application context was closed and produced:

```text
Probe scheduler shutting down
```

This demonstrated the destruction callback during context cleanup. It did not mean that the web server had started successfully in that attempt; a later run started successfully.

### D. Prototype scope: `ProbeTask`

**File:** `src/main/java/com/chandu/roost/service/ProbeTask.java`

The experiment used a prototype-scoped component:

```java
@Component
@Scope("prototype")
public class ProbeTask {
    // Constructor logs instance creation.
}
```

Its constructor logged:

```text
ProbeTask instance created
```

This allowed instance creation to be counted directly in the application logs.

#### Experiment 1: Direct constructor injection

Initially, `ProbeScheduler` held a direct reference:

```java
private final ProbeTask probeTask;

public ProbeScheduler(ProbeTask probeTask) {
    this.probeTask = probeTask;
}
```

Although `ProbeTask` was prototype-scoped, Spring created the scheduler as a singleton and resolved its `ProbeTask` dependency when constructing it.

The scheduler retained that injected reference.

Calling `runProbeTask()` three times produced:

- `ProbeTask instance created`: 1 time.
- `Running probe task`: 3 times.

**Conclusion:** Directly injecting a prototype bean into a singleton does not automatically produce a new instance on each method call.

#### Experiment 2: Using `ObjectProvider`

The direct dependency was replaced with:

```java
private final ObjectProvider<ProbeTask> probeTaskProvider;

public ProbeScheduler(
        ObjectProvider<ProbeTask> probeTaskProvider) {
    this.probeTaskProvider = probeTaskProvider;
}
```

The task method requested an instance explicitly:

```java
public void runProbeTask() {
    ProbeTask task = probeTaskProvider.getObject();
    log.info("Running probe task");
}
```

Each invocation of `getObject()` requests a new prototype instance from Spring.

**Verified result:** Calling `runProbeTask()` three times produced three `ProbeTask instance created` messages and three `Running probe task` messages.

The application started successfully on port 8080.

**Conclusion:** `ObjectProvider<T>` provides a way to request an instance when needed. It does not change the bean's declared scope.

### E. Bean lifecycle interfaces

`@PostConstruct` and Spring's `InitializingBean.afterPropertiesSet()` both provide initialization hooks after dependency injection.

For ordinary application initialization, `@PostConstruct` is often preferable because the bean does not need to implement a Spring-specific lifecycle interface.

Use framework-specific lifecycle interfaces when their explicit contracts are useful for the component.

## 4. Interview Points

**Q1. What problem does Spring AOP solve?**

AOP separates cross-cutting concerns, such as logging and timing, from the business methods to which those concerns apply.

**Q2. What is `@Around` advice?**

`@Around` advice surrounds a method invocation. It can run before and after the target, decide whether to proceed, inspect the result, and handle exceptions.

**Q3. Why does `ProceedingJoinPoint.proceed()` matter?**

It proceeds with the intercepted invocation. If the advice does not call it, the target invocation normally does not occur through that advice.

**Q4. Why might Spring AOP advice not run during self-invocation?**

The internal method call bypasses the Spring proxy and invokes the method directly on the current object.

**Q5. When does `@PostConstruct` run?**

After dependency injection, during bean initialization and before the bean is put into service.

**Q6. What is the difference between `@PostConstruct` and `InitializingBean.afterPropertiesSet()`?**

Both are initialization callbacks. `@PostConstruct` is annotation-based; `afterPropertiesSet()` requires implementing a Spring-specific interface.

**Q7. What does `@PreDestroy` do?**

It defines a callback that Spring invokes during managed bean destruction, such as when the application context shuts down gracefully.

**Q8. Name the main Spring bean scopes.**

`singleton`, `prototype`, `request`, `session`, `application`, and `websocket`.

**Q9. Why does direct prototype injection into a singleton reuse the same object?**

The prototype is resolved during singleton construction, and the singleton retains the injected reference.

**Q10. How does `ObjectProvider<T>` help?**

It lets a bean request an instance from Spring when needed, such as by calling `getObject()` for each new prototype task.

**Q11. Does `ObjectProvider<T>` change the scope of the underlying bean?**

No. The bean retains its configured scope. The provider changes when the application requests an instance.

**Q12. Does Spring automatically destroy prototype beans when they are no longer used?**

Spring does not automatically manage the full destruction lifecycle of prototype instances after handing them to the caller. Resource cleanup must be handled appropriately.

## 5. Common Mistakes

- **Putting cross-cutting logic in every business method:** Prefer a suitable aspect when centralized interception is appropriate.
- **Assuming every method call is intercepted:** Proxy-based AOP has limitations, particularly with self-invocation.
- **Forgetting `proceed()`:** An `@Around` advice that does not proceed may prevent the target method from running.
- **Confusing construction with initialization:** `@PostConstruct` is called after dependency injection, not before it.
- **Assuming prototype means a new object on every use:** It means a new instance when the container resolves the bean.
- **Directly injecting a prototype into a singleton and expecting fresh instances:** Use `ObjectProvider<T>` or another suitable lookup mechanism when fresh instances are required.
- **Assuming `ObjectProvider` guarantees thread safety:** Each instance can still contain mutable, non-thread-safe state.
- **Assuming Spring fully manages prototype destruction:** The caller must ensure resources are released appropriately.
- **Treating an application log message as proof of full startup:** Verify the final startup status and any subsequent errors.

### Testing lesson from today's work

The prototype-scope experiment used constructor logging to observe actual instance creation. This was more reliable than inferring instance behavior from the annotations alone.

The lifecycle experiment also showed that `@PreDestroy` can run when Spring closes the application context after a startup failure. The callback's presence does not prove that the entire application started successfully.

## 6. Time Complexity and Performance

AOP, lifecycle callbacks, and bean scopes do not have a single universal time complexity.

- **AOP:** Proxy interception and advice introduce runtime overhead. The cost depends on the proxy, advice, and work performed.
- **Lifecycle:** Initialization and destruction costs depend on the operations performed by callbacks.
- **Scopes:** Resolving a singleton generally reuses an existing instance after creation. Resolving a prototype entails creating another instance, including its constructor and applicable initialization work.

Measure the actual behavior when performance matters. Do not infer end-to-end application performance from bean scope alone.

## 7. When to Use in Production

### AOP

Useful for cross-cutting concerns such as execution-time logging, tracing, and selected auditing or instrumentation tasks.

Avoid using AOP when explicit, ordinary method calls would be clearer or when the behavior depends on interception paths that proxy-based AOP cannot reliably cover.

### Lifecycle callbacks

Use initialization callbacks for setup that genuinely belongs to the bean lifecycle. Use destruction callbacks for appropriate shutdown actions and resource cleanup.

For background jobs or external resources, ensure initialization and shutdown behavior is compatible with application startup, failure handling, and graceful termination.

### Prototype scope and `ObjectProvider`

Useful when a singleton component needs fresh, stateful helper objects for individual operations.

For example, a scheduler may coordinate many probe jobs while each job receives its own task object.

Do not use prototype scope merely because a component performs work. Stateless singleton services are often sufficient. Choose the scope based on the required object lifecycle and state ownership.

## 8. Quick Revision

- AOP centralizes cross-cutting behavior.
- `@Around` advice surrounds an invocation; `proceed()` invokes the target through the advice.
- Spring AOP normally uses proxies, so self-invocation can bypass advice.
- `@PostConstruct` runs after dependency injection and before the bean is put into service.
- `@PreDestroy` runs during managed bean destruction.
- `singleton` means one instance per application context.
- `prototype` creates a new instance when the container resolves the bean.
- A singleton directly injected with a prototype retains that injected instance.
- `ObjectProvider<T>.getObject()` requests an instance when needed.
- `ObjectProvider` does not guarantee thread safety or full prototype destruction management.
- Verify actual runtime behavior through logs and tests.

## 9. Self-Check Q&A

**1. What is the purpose of AOP?**

To separate cross-cutting concerns from business logic and apply them consistently where appropriate.

**2. Why use `@Around` instead of `@Before` for execution-time measurement?**

`@Around` can measure both before and after the target invocation. `@Before` alone cannot measure the completed invocation's duration.

**3. Why might self-invocation bypass an aspect?**

The call does not pass through the Spring proxy.

**4. When does `@PostConstruct` run?**

After dependency injection, during initialization and before the bean is put into service.

**5. Why did the direct-injection experiment create one `ProbeTask` instance?**

Spring created the prototype dependency while constructing the singleton scheduler, which retained that instance.

**6. Why did the `ObjectProvider` experiment create three instances?**

The scheduler called `getObject()` three times, requesting a new prototype instance each time.

**7. Does `ObjectProvider` change prototype scope?**

No. It provides a way to request instances from Spring when needed.

**8. Who is responsible for resources owned by a prototype bean?**

The code or lifecycle mechanism responsible for the resource must ensure appropriate cleanup.

**9. Is `@PreDestroy` proof that application startup succeeded?**

No. It can run when the application context closes after either normal operation or a failed startup attempt.

**10. What is the main distinction between bean scope and method invocation?**

Bean scope governs instance resolution and reuse, not how often an existing instance's methods are called.

## 10. Key Takeaway

**Spring's runtime behavior becomes predictable when you distinguish interception, lifecycle, and instance scope.**

In Roost, AOP measured method execution without duplicating timing code. Lifecycle callbacks demonstrated managed initialization and shutdown. The prototype experiment proved that direct injection into a singleton retains one reference, while `ObjectProvider` allows the singleton to request fresh instances when required.

Use each mechanism intentionally, verify its behavior, and account for its limitations in production code.
