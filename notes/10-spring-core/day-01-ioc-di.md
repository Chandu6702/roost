# IoC & Dependency Injection

**Week 1 · Day 1** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

**Inversion of Control (IoC)** is a design principle where control over creating and managing objects is transferred from application code to an external mechanism. **Dependency Injection (DI)** is a specific form of IoC where a class receives the dependencies it needs from outside instead of creating them itself. Spring Boot uses an IoC container to create, configure, and manage application objects and their dependencies. This reduces coupling and makes components easier to test and maintain.

## 2. Mental Model

Without DI:

```text
ProbeRunner
    |
    | creates
    ↓
new HttpProbeClient()
```

`ProbeRunner` controls its own dependency.

With constructor DI:

```text
External code / Test
        |
        | creates
        ↓
HttpProbeClient
        |
        | injects
        ↓
ProbeRunner
```

The important idea:

> **The class that needs a dependency does not create it; somebody outside provides it.**

For testing:

```text
Production:
ProbeRunner → HttpProbeClient

Test:
ProbeRunner → MockHttpProbeClient
```

The same `ProbeRunner` can work with a different dependency because the dependency is supplied from outside.

Later, Spring's IoC container will take over the external creation and injection:

```text
Spring Container
      |
      | creates
      ↓
HttpProbeClient
      |
      | injects
      ↓
ProbeRunner
```

## 3. My Roost Code

### Files

* `src/main/java/com/chandu/roost/HttpProbeClient.java`
* `src/main/java/com/chandu/roost/ProbeRunner.java`
* `src/test/java/com/chandu/roost/ProbeRunnerTest.java`

Representative code:

```java
public class ProbeRunner {

    private final HttpProbeClient httpProbeClient;

    public ProbeRunner(HttpProbeClient httpProbeClient) {
        // Dependency is provided from outside instead of being created here.
        this.httpProbeClient = httpProbeClient;
    }

    public String run(String url) {
        return httpProbeClient.probe(url);
    }
}
```

The test injects a test double:

```java
MockHttpProbeClient mockClient = new MockHttpProbeClient();

ProbeRunner runner = new ProbeRunner(mockClient);

String result = runner.run("https://example.com");

assertEquals("Mock response", result);
```

No Spring annotations were used on Day 1.

## 4. Interview Points

* **IoC** is a principle where control over object creation/management is transferred from application code to an external mechanism such as a framework/container.
* **Dependency Injection** is a specific form of IoC where dependencies are provided from outside instead of being created inside the dependent class.
* **Constructor injection is generally preferred** because required dependencies are explicit, can be `final`, are enforced by the compiler, and are easy to provide in tests.
* DI improves **testability** because a real dependency can be replaced with a fake/mock without changing the class under test.
* **Service Locator** hides dependencies because the class looks them up from a central registry; DI makes dependencies explicit and avoids coupling the class to the locator.

## 5. Common Mistakes

* **Thinking IoC simply means loose coupling** — loose coupling is a benefit of IoC, but IoC specifically means transferring control of object creation/management.
  See `mistakes.md` — IoC definition.

* **Calling DI "Data Injection"** — the correct term is **Dependency Injection**.
  See `mistakes.md` — DI terminology.

* **Creating the dependency inside the dependent class** — `ProbeRunner` should not do `new HttpProbeClient()`. The dependency should be supplied from outside.

* **Confusing a real dependency with a test double** — `new HttpProbeClient()` creates the real implementation; a mock/fake is an alternative object supplied during testing.

* **Injecting a value and then ignoring it** — the original implementation accepted a `url` in the constructor but hardcoded a different URL inside `run()`. Dependencies/inputs that are provided should actually be used correctly.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

IoC means transferring control of object creation and management from application code to an external mechanism. DI is a specific form of IoC where dependencies are supplied to a class from outside instead of being created inside it. Constructor injection makes dependencies explicit, supports `final` fields, and makes testing easy because real dependencies can be replaced with mocks or fakes.

### Q&A

**Q1. What is IoC in one sentence?**

**A.** IoC is a principle where control over creating and managing objects is transferred from application code to an external mechanism such as a framework or container.

**Q2. DI is a specific form of IoC — of what?**

**A.** Dependency Injection is a specific form of IoC where an object's dependencies are provided from outside instead of the object creating them itself.

**Q3. Constructor injection vs field injection — which is generally better and why?**

**A.** Constructor injection is generally preferred because required dependencies are explicit, can be `final`, are enforced by the compiler, and are easy to provide directly in tests. Field injection hides dependencies and makes testing more cumbersome.

**Q4. Why does DI make code more testable?**

**A.** Because the dependency can be replaced with a fake or mock. In our example, `ProbeRunner` receives `MockHttpProbeClient` instead of the real `HttpProbeClient`, so the test doesn't need a real HTTP operation.

**Q5. What is the Service Locator anti-pattern and why is DI preferred?**

**A.** Service Locator makes a class ask a central registry for its dependencies. This hides dependencies and couples the class to the locator. DI makes dependencies explicit and provides them from outside.

**Q6. Does DI mean that we never create dependency objects ourselves?**

**A.** No. DI means the **dependent class does not create its own dependency**. Another part of the application, a test, or eventually the Spring container creates the dependency and supplies it.

**Q7. What is the difference between IoC and DI?**

**A.** IoC is the broader principle of transferring control to an external mechanism. DI is one technique for achieving IoC by providing dependencies from outside the class.

**Q8. Why is `new HttpProbeClient()` inside `ProbeRunner` undesirable?**

**A.** It tightly couples `ProbeRunner` to the concrete dependency and makes replacing that dependency during testing difficult. Constructor injection removes that responsibility from `ProbeRunner`.
