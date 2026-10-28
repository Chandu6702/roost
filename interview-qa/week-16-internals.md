# Week 16 — Spring Internals Interview Q&A

**Topics:** AOP · Bean lifecycle and scopes · Custom auto-configuration · Caching and Redis · Scheduling and virtual threads

## 1. AOP

### Q1. What is Aspect-Oriented Programming (AOP), and why is it useful?

A: AOP separates cross-cutting concerns, such as execution-time logging, from core business logic. It allows reusable behavior to be applied to selected method executions without duplicating that code in every method.

### Q2. What is the difference between an aspect, advice, and a pointcut?

A:
- **Aspect:** Groups cross-cutting behavior.
- **Advice:** Defines the behavior that executes and when it executes.
- **Pointcut:** Selects the method executions where the advice applies.

### Q3. What does `@Around` advice do, and what is a limitation of Spring's proxy-based AOP?

A: `@Around` advice wraps a method invocation and can execute code before and after it, proceed with the invocation, inspect its result, or handle exceptions. With proxy-based Spring AOP, self-invocation generally bypasses the proxy, so the inner method call does not trigger the proxy advice.

## 2. Bean Lifecycle and Scopes

### Q4. What is the difference between singleton and prototype bean scope?

A: A singleton bean normally has one instance per Spring application context. A prototype bean is created again whenever it is requested from the container. Spring does not generally manage a prototype bean's complete destruction lifecycle after handing it to the caller.

### Q5. Why use `ObjectProvider<T>` instead of directly injecting a prototype bean?

A: Injecting a prototype directly into a singleton normally resolves the prototype when the singleton is created, so repeated use of that injected reference does not automatically produce new instances. `ObjectProvider.getObject()` allows the singleton to request a new prototype instance when needed.

## 3. Custom Auto-Configuration

### Q6. How does Spring Boot discover custom auto-configuration?

A: The configuration class is marked with `@AutoConfiguration` and listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. The annotation defines the configuration class, while the imports metadata enables discovery.

### Q7. How do `@ConditionalOnProperty` and `@ConditionalOnMissingBean` differ?

A:
- `@ConditionalOnProperty` activates configuration when a property meets the specified condition.
- `@ConditionalOnMissingBean` allows a default bean definition to back off when a matching bean is already visible during condition evaluation.

### Q8. How does Roost's custom webhook auto-configuration work?

A: `WebhookAutoConfiguration` enables `WebhookProperties` and conditionally registers a `WebhookService` bean. The `roost.webhook.enabled` property controls activation, while `@ConditionalOnMissingBean(WebhookService.class)` allows an application-provided service to replace the default. The class is discovered through `AutoConfiguration.imports`.

## 4. Caching and Redis

### Q9. What happens when a method annotated with `@Cacheable` is called?

A: Spring checks the configured cache using the computed key. On a cache hit, it returns the cached value without invoking the method body. On a cache miss, it invokes the method and stores the result if caching succeeds.

### Q10. What is the difference between `@Cacheable` and `@CacheEvict`?

A: `@Cacheable` reuses and stores results to avoid repeated computation or data access. `@CacheEvict` invalidates cached entries so later requests can load current data.

### Q11. How did Roost implement and verify workspace monitor caching?

A: `WorkspaceMonitorCacheService` uses `@Cacheable` with cache name `workspaceMonitors` and `#workspaceId` as its key. `MonitorCacheDto` provides the cache representation. Runtime logs confirmed a miss, a database query, cache entry creation, and a subsequent hit.

### Q12. Why does Roost evict the workspace monitor cache after create, update, or delete?

A: These operations can make the cached list stale. `MonitorService` uses `@CacheEvict(cacheNames = "workspaceMonitors", allEntries = true)` so the next read reloads the list. Runtime verification showed the update triggering invalidation, followed by a miss, database reload, and new cache entry.

### Q13. Why can Redis caching fail even when the repository query succeeds?

A: The configured cache serializer may be unable to serialize the returned object. In Roost, the initial runtime request failed during cache serialization; making `MonitorCacheDto` implement `Serializable` resolved the problem with the configured serialization behavior. The required approach depends on the actual serializer configuration.

## 5. Scheduling, Async Execution, and Virtual Threads

### Q14. What is the difference between scheduling and asynchronous execution?

A: Scheduling determines when work becomes due. Asynchronous execution allows the work to run separately from the caller or scheduling thread, preventing task execution from unnecessarily blocking scheduling coordination.

### Q15. What are Java virtual threads, and what do they not solve?

A: Virtual threads are lightweight threads managed by the Java runtime and can help applications scale workloads involving many blocking operations. They do not remove limits imposed by CPU, database connection pools, remote services, or network capacity.

### Q16. Explain Roost's scheduled probe execution flow.

A: `ProbeScheduler` finds due schedules and dispatches probe work. The task executes the probe through the relevant service path, and the result is persisted. Runtime logs confirmed schedule dispatch, probe execution, and result inserts; the virtual-thread check also reported `virtual=true`.

## 6. Integration and Design

### Q17. How do these Spring internals features complement each other in a real application?

A: AOP handles cross-cutting behavior, bean scopes control object creation, and auto-configuration registers conditional defaults. Caching reduces repeated reads while eviction maintains data freshness. Scheduling and asynchronous execution handle recurring background work. Each mechanism has a distinct responsibility, and runtime behavior depends on how these boundaries interact.

### Q18. Why are automated tests and runtime verification both important?

A: Automated tests validate defined scenarios and expected behavior. Runtime verification checks that the actual application context, proxies, cache infrastructure, configuration, and request paths behave as expected. They complement each other; neither replaces the other.

## Key Takeaway

For each topic, be ready to explain **the problem it solves, when it executes, and how Roost uses or verifies it**. Focus on understanding runtime behavior and design trade-offs instead of memorizing annotations in isolation.
