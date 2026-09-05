# Week 10 — Spring Core Interview Q&A

**Topics:** IoC, DI, Spring Beans, Component Scanning, Auto-Configuration, `@ConfigurationProperties`, Profiles, Flyway

## IoC & Dependency Injection

**Q1. What is IoC in Spring?**

**A.** Inversion of Control means the Spring container takes responsibility for creating and managing objects and their dependencies instead of application classes creating them directly.

---

**Q2. What is Dependency Injection, and how is it related to IoC?**

**A.** Dependency Injection is a specific form of IoC where an object's dependencies are supplied from outside rather than the object creating or locating them itself.

---

**Q3. Why is constructor injection preferred over field injection?**

**A.** Constructor injection makes dependencies explicit, supports `final` fields, guarantees required dependencies are provided during object construction, and makes unit testing easy without reflection or Spring.

---

**Q4. How does Dependency Injection improve testability?**

**A.** A class can receive a fake or mock dependency through its constructor, allowing it to be tested independently of the real implementation.

```java
ProbeRunner runner = new ProbeRunner(new MockHttpProbeClient());
```

---

## Beans & Spring Container

**Q5. What is a Spring bean?**

**A.** A Spring bean is an object whose creation and lifecycle are managed by the Spring IoC container.

---

**Q6. What is the difference between `@Component`, `@Service`, and `@Repository`?**

**A.** All are component stereotypes used for Spring bean discovery. `@Service` communicates business/service-layer intent, while `@Repository` represents the persistence layer and also participates in persistence exception translation. `@Component` is the generic stereotype.

---

**Q7. What happens if Spring finds multiple beans matching one dependency?**

**A.** Dependency injection becomes ambiguous and Spring throws `NoUniqueBeanDefinitionException` unless the ambiguity is resolved with `@Primary` or `@Qualifier`.

---

**Q8. `@Primary` vs `@Qualifier` — when would you use each?**

**A.**

- `@Primary` → designate the default bean when multiple candidates exist.
- `@Qualifier` → explicitly select a particular bean at an injection point.

---

**Q9. What does component scanning do, and what does `@SpringBootApplication` scan by default?**

**A.** Component scanning discovers classes annotated with Spring stereotypes and registers them as bean definitions. By default, scanning starts from the package containing the `@SpringBootApplication` class and includes its subpackages.

---

## Spring Boot

**Q10. What does `@SpringBootApplication` combine?**

**A.**

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

It identifies the application configuration, enables Boot's auto-configuration, and enables component scanning.

---

**Q11. What is Spring Boot auto-configuration?**

**A.** Auto-configuration automatically configures application infrastructure based on the dependencies on the classpath, existing beans, and application configuration.

For example, web dependencies allow Spring Boot to configure the embedded web server and Spring MVC infrastructure automatically.

---

**Q12. What is the difference between component scanning and auto-configuration?**

**A.**

**Component scanning** discovers **our application components**, such as `@Service` and `@RestController`.

**Auto-configuration** configures **Spring/framework infrastructure** based on conditions such as classpath dependencies and existing configuration.

---

## Configuration & Profiles

**Q13. Why use `@ConfigurationProperties` instead of multiple `@Value` annotations?**

**A.** `@ConfigurationProperties` groups related configuration into a type-safe object, making configuration cleaner, easier to validate, test, and maintain. `@Value` is more suitable for individual properties.

---

**Q14. How do Spring profiles work with `application.yml`?**

**A.** `application.yml` provides common/default configuration, while an active profile such as `dev` or `prod` can override specific properties.

For Roost:

```text
Base → interval = 60
Dev  → interval = 30
Prod → interval = 300
```

Properties not overridden by the active profile continue to use the base value.

---

**Q15. Does creating `application-dev.yml` automatically activate the dev profile?**

**A.** No. The file defines configuration for the `dev` profile, but the profile must be activated separately, for example with:

```text
--spring.profiles.active=dev
```

---

## Flyway

\*\*Q16. What problem does Flyway solve?

**A.** Flyway manages database schema evolution using versioned migration scripts. It applies migrations in order and records which migrations have already been executed.

---

**Q17. Flyway vs `ddl-auto=update` — why prefer Flyway?**

**A.** Flyway uses explicit, version-controlled, ordered migrations with a migration history. `ddl-auto=update` attempts to infer schema changes from JPA mappings at runtime and does not provide the same level of control, reproducibility, or reviewability.

---

**Q18. What is `flyway_schema_history`?**

**A.** It is Flyway's metadata table that records applied migrations so Flyway knows which migrations have already been executed.

---

\*\*Q19. Where does Flyway look for migrations by default?

**A.**

```text
src/main/resources/db/migration
```

For example:

```text
V1__workspaces.sql
```

---

## Scenario Questions

**Q20. A class has two implementations of an interface and Spring throws `NoUniqueBeanDefinitionException`. How would you fix it?**

**A.** First determine whether one implementation should be the default. If yes, use `@Primary`. If different injection points need different implementations, use `@Qualifier` to explicitly select the required bean.

---

**Q21. Your `@ConfigurationProperties` values are not what you expect in `dev`. What would you check?**

**A.**

1. Is the `dev` profile actually active?
2. Is the property path correct?
3. Does the YAML property match the `@ConfigurationProperties` prefix?
4. Is another configuration source overriding the value?
5. Is the binding class registered correctly?

---

**Q22. Your Flyway migration isn't being executed. What would you check?**

**A.**

1. Is the migration under `db/migration`?
2. Is the filename valid, such as `V1__workspaces.sql`?
3. Is Flyway enabled?
4. Is Spring connecting to the expected database?
5. Does `flyway_schema_history` show that the migration was already applied?

---

## Must-Know Summary

Before the interview, I should be able to explain these without hesitation:

- **IoC** → container manages object creation/dependencies.
- **DI** → dependencies are supplied from outside.
- **Constructor injection** → explicit, safe, testable.
- **Bean** → object managed by Spring.
- **Component scanning** → discovers application components.
- **Auto-configuration** → configures framework infrastructure.
- **`@Primary` vs `@Qualifier`** → default candidate vs explicit selection.
- **`@ConfigurationProperties`** → grouped, type-safe configuration.
- **Profiles** → environment-specific configuration overrides.
- **Flyway** → version-controlled database schema evolution.
- **Flyway vs `ddl-auto`** → explicit migrations vs inferred runtime schema updates.
