# Beans, `@Service`, Component Scanning & Dependency Wiring

**Week 1 · Day 3** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

A **Spring Bean** is an object whose creation and lifecycle are managed by the Spring IoC container. Annotations such as `@Component`, `@Service`, and `@Repository` allow Spring to discover classes through component scanning and register them as beans. This removes the need to manually create dependencies with `new` and allows Spring to manage object creation and dependency wiring.

## 2. Mental Model

Think of the Spring IoC container as an **object factory + dependency wiring system**.

```text
@SpringBootApplication
        ↓
@ComponentScan
        ↓
Find Spring components
        ↓
Register bean definitions
        ↓
Create singleton beans
        ↓
Resolve dependencies
        ↓
Inject dependencies
```

For Roost:

```text
WorkspaceController
        │
        │ requires
        ▼
WorkspaceService
        │
        ▼
creates Workspace
```

Spring creates `WorkspaceService` and `WorkspaceController`. When creating the controller, Spring sees that its constructor requires `WorkspaceService`, finds the corresponding bean, and injects it.

### Important distinction

> **Component scanning discovers/registers components. The Spring container creates the beans and wires their dependencies.**

## 3. My Roost Code

### Files

- `src/main/java/com/chandu/roost/workspace/WorkspaceController.java`
- `src/main/java/com/chandu/roost/workspace/WorkspaceService.java`
- `src/main/java/com/chandu/roost/workspace/Workspace.java`
- `src/main/java/com/chandu/roost/workspace/CreateWorkspaceRequest.java`

### Representative code

```java
@Service
public class WorkspaceService {
    public Workspace createWorkspace(String name) {
        String id = UUID.randomUUID().toString();
        return new Workspace(id, name);
    }
}
```

`@Service` makes `WorkspaceService` discoverable as a Spring bean.

Constructor injection in the controller:

```java
public WorkspaceController(WorkspaceService service) {
    this.service = service;
}
```

Spring supplies the `WorkspaceService` dependency instead of the controller creating it with `new`.

The request and response models were implemented as Java records:

```java
public record CreateWorkspaceRequest(String name) {}

public record Workspace(String id, String name) {}
```

The endpoint successfully returned HTTP **201 Created** with the generated workspace JSON.

## 4. Interview Points

- A **Spring Bean** is an object managed by the Spring IoC container.
- `@Component` is the generic stereotype for a Spring-managed component.
- `@Service` is a specialized `@Component` used to communicate a service/business-logic role.
- `@Repository` is a specialized `@Component` used for persistence/data-access classes and participates in persistence exception translation.
- By default, singleton beans are created eagerly during application-context startup. `@Lazy` can defer creation.
- If multiple beans match the same injection type, Spring cannot choose automatically and may throw `NoUniqueBeanDefinitionException`.
- `@Primary` designates the default candidate among multiple beans.
- `@Qualifier` explicitly selects a particular bean at an injection point.
- `@ComponentScan` scans the package containing the `@SpringBootApplication` class and its subpackages by default.
- A single constructor does not require `@Autowired`.
- Constructor injection makes required dependencies explicit and allows them to be stored in `final` fields.

## 5. Common Mistakes

- **Confusing component scanning with bean instantiation** — component scanning discovers and registers components; the Spring container then creates the bean instances and wires dependencies.
- **Not recognizing multiple-bean ambiguity** — if multiple beans implement the same interface, Spring needs `@Primary` or `@Qualifier` to resolve the injection.
- **Forgetting the default component-scan scope** — Spring scans from the package containing the `@SpringBootApplication` class and its subpackages, not the entire project.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

Spring's component scanning discovers classes such as `@Service` and `@RestController` and registers them as bean definitions. The IoC container creates the beans and resolves their dependencies, commonly through constructor injection. In Roost, Spring creates `WorkspaceService`, sees that `WorkspaceController` requires it, and injects the service automatically.

### Q&A

**Q1. What is a Spring Bean?**

**A.** A Java object whose creation and lifecycle are managed by the Spring IoC container.

**Q2. What is the difference between `@Component`, `@Service`, and `@Repository`?**

**A.** All are stereotype annotations used for component discovery. `@Component` is generic, `@Service` communicates a service/business-logic role, and `@Repository` communicates a persistence/data-access role and provides persistence exception translation.

**Q3. When are singleton beans normally created?**

**A.** By default, singleton beans are created eagerly during application-context startup. `@Lazy` can defer creation.

**Q4. What happens when two beans implement the same interface and that interface is constructor-injected?**

**A.** Spring finds multiple candidates and cannot determine which one to inject, resulting in `NoUniqueBeanDefinitionException` unless the ambiguity is resolved.

**Q5. How do `@Primary` and `@Qualifier` resolve multiple beans?**

**A.** `@Primary` makes one bean the default candidate. `@Qualifier` explicitly selects a particular bean at an injection point.

**Q6. What does `@ComponentScan` scan by default?**

**A.** The package containing the `@SpringBootApplication` class and all its subpackages.

**Q7. Why didn't `WorkspaceController` need `new WorkspaceService()`?**

**A.** Because `WorkspaceService` is a Spring bean. Spring discovers it, creates it, and injects it into the controller's constructor.

**Q8. Why is constructor injection preferred?**

**A.** It makes dependencies explicit, ensures required dependencies are supplied during construction, supports `final` fields, and makes the class easier to test.

**Q9. Why is `private final WorkspaceService service` useful?**

**A.** `private` provides encapsulation, while `final` prevents the dependency reference from being reassigned after construction.

**Q10. What is the difference between component scanning and bean creation?**

**A.** Component scanning discovers and registers components as bean definitions. The Spring container subsequently creates and manages the actual bean instances.

**Q11. Does `@Service` itself contain special business-logic behavior?**

**A.** No. It is primarily a specialized stereotype of `@Component` that communicates the class's role as a service. Spring uses it for component discovery and bean registration.

**Q12. Why does `WorkspaceService` use `new Workspace(...)` even though we are learning dependency injection?**

**A.** `Workspace` is a result/value object created as part of the service's operation. `WorkspaceService`, on the other hand, is a dependency managed by Spring and therefore should not be manually created by the controller.
