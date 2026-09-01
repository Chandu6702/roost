# First Roost Endpoint — Health + Application Configuration

**Week 1 · Day 2** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

Spring Boot's web support allows a Java application to expose HTTP endpoints without manually configuring a web server, servlet infrastructure, or JSON serialization. A `@RestController` class defines REST endpoints, while Spring Boot's auto-configuration detects web dependencies and configures the required infrastructure. Today Roost exposes a simple `/health` endpoint so we can understand the basic Spring MVC request flow before introducing services and dependency injection into the web application.

## 2. Mental Model

```text
Browser / curl
      |
      | GET /health
      ↓
Embedded Tomcat :8080
      |
      ↓
Spring MVC / DispatcherServlet
      |
      ↓
HealthController
      |
      ↓
Map<String, String>
      |
      ↓
HTTP message conversion
      |
      ↓
JSON response
```

`@SpringBootApplication` provides the major startup capabilities:

```text
@SpringBootApplication
        |
        +-- @SpringBootConfiguration
        +-- @EnableAutoConfiguration
        +-- @ComponentScan
```

Auto-configuration reacts to dependencies on the classpath.

For example:

```text
spring-boot-starter-webmvc
        ↓
web infrastructure configured

spring-boot-starter-data-jpa
        ↓
DataSource configuration attempted
        ↓
failed because no database was configured
```

## 3. My Roost Code

**File:** `src/main/java/com/chandu/roost/health/HealthController.java`

```java
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> getHealth() {
        // Spring serializes the returned Map directly into JSON.
        return Map.of("status", "up", "app", "roost");
    }
}
```

**Configuration:** `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: roost

server:
  port: 8080
```

The endpoint was tested with:

```text
GET http://localhost:8080/health
```

and returned:

```json
{
  "status": "up",
  "app": "roost"
}
```

## 4. Interview Points

- `@SpringBootApplication` combines `@SpringBootConfiguration`, `@EnableAutoConfiguration`, and `@ComponentScan`.
- Spring Boot uses embedded Tomcat by default for a typical MVC web application; `server.port` controls the listening port.
- `@RestController` is effectively `@Controller + @ResponseBody`, so return values are written directly to the HTTP response body.
- Auto-configuration uses the classpath and configuration to automatically configure appropriate Spring infrastructure.
- Component scanning discovers classes such as `HealthController` under the application's base package.

## 5. Common Mistakes

- **Confusing localhost with the web server** — `localhost` is the hostname, `8080` is the port, and Tomcat is the embedded web server. See `mistakes.md`.

- **Incomplete distinction between `@Controller` and `@RestController`** — `@Controller` is commonly used for MVC views, while `@RestController` returns response data directly through `@ResponseBody`. See `mistakes.md`.

- **Assuming YAML is required by Spring Boot** — Spring Boot supports both `application.properties` and `application.yml`. YAML was used today because the roadmap intentionally introduced it.

- **Assuming auto-configuration configures dependencies themselves** — auto-configuration configures Spring application infrastructure based on available dependencies and conditions.

- **Forgetting that classpath dependencies can trigger auto-configuration** — JPA caused Spring Boot to attempt DataSource configuration even though Roost had no database configured yet.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

Spring Boot's `@SpringBootApplication` enables configuration, auto-configuration, and component scanning. A `@RestController` maps HTTP requests to Java methods and returns response data directly, which Spring can serialize to JSON. Auto-configuration detects dependencies such as Spring MVC and configures the required infrastructure, including the embedded web server.

**Q1. What does `@SpringBootApplication` combine?**

**A.** `@SpringBootConfiguration`, `@EnableAutoConfiguration`, and `@ComponentScan`.

**Q2. What web server runs by default in a typical Spring Boot MVC application?**

**A.** Embedded Tomcat. It is provided through the web starter dependencies. `server.port` controls the port on which it listens.

**Q3. What is the difference between `localhost`, port `8080`, and Tomcat?**

**A.** `localhost` is the hostname for the local machine, `8080` is the network port, and Tomcat is the embedded web server handling HTTP requests.

**Q4. What is the difference between `@Controller` and `@RestController`?**

**A.** `@Controller` is commonly used for MVC applications that return views, while `@RestController` combines `@Controller` with `@ResponseBody` so handler return values are written directly to the HTTP response body.

**Q5. What is Spring Boot auto-configuration?**

**A.** It automatically configures appropriate Spring infrastructure based on dependencies and configuration available on the classpath, subject to conditions.

**Q6. Why did the application initially fail because of JPA?**

**A.** The JPA dependency caused Spring Boot to attempt DataSource auto-configuration. No database URL or suitable embedded database was configured, so startup failed.

**Q7. Why did `/health` work without manually configuring Tomcat or JSON conversion?**

**A.** Spring Boot's auto-configuration configured the web infrastructure, while component scanning discovered `HealthController`. Spring MVC then handled the request and serialized the returned `Map` into JSON.

**Q8. Why did adding Spring Security cause `/health` to require login?**

**A.** Spring Security was present on the classpath, so Spring Boot automatically configured security. The default security configuration protected the application's endpoints, including `/health`.

**Q9. What does `@GetMapping("/health")` do?**

**A.** It maps HTTP GET requests sent to `/health` to the annotated controller method.

**Q10. Does Spring Boot require `application.yml` instead of `application.properties`?**

**A.** No. Both are supported configuration formats. They can represent the same Spring Boot configuration properties.
