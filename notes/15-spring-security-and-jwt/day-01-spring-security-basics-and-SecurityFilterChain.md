# Spring Security Basics & SecurityFilterChain

**Week 15 · Day 1** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

Spring Security provides authentication, authorization, and other security controls for Spring applications. Incoming HTTP requests pass through a configured `SecurityFilterChain` before reaching Spring MVC controllers. In Roost, this gives us a security boundary in front of the REST API: health and API documentation remain public, while application endpoints require authentication. This is the foundation for the JWT-based security architecture we will build in the following days.

## 2. Mental Model

Think of `SecurityFilterChain` as the security gate in front of the controllers.

```text
HTTP Request
     │
     ▼
SecurityFilterChain
     │
     ├── Security context
     ├── Authentication
     ├── Authorization
     ├── CSRF protection
     ├── Session management
     └── Security exception handling
     │
     ▼
Spring MVC
     │
     ▼
Controller
     │
     ▼
Service → Repository → PostgreSQL
```

The four concepts to remember:

```text
Authentication → WHO are you?
Authorization  → WHAT are you allowed to do?
CSRF           → Did another site trick the browser into
                 making an authenticated request?
Session        → Where/how is authentication state maintained?
```

For Roost:

```text
JWT + Authorization header
        ↓
Stateless authentication
        ↓
No server-side HTTP session for authentication
```

The exact filters in the chain are feature/configuration dependent. Spring Security does **not** mean every possible security filter is always enabled.

## 3. My Roost Code

### `pom.xml`

Security was previously disabled in Roost. For Week 15, it was enabled:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### `src/main/java/com/chandu/roost/config/SecurityConfig.java`

```java
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
```

Key configuration:

* `permitAll()` → selected endpoints don't require authentication.
* `authenticated()` → remaining endpoints require an authenticated principal.
* `STATELESS` → Spring Security does not use the HTTP session to persist authentication state.
* CSRF is disabled because Roost is being designed as a stateless bearer-token API using the `Authorization` header.

Current policy:

```text
/health/**          → public
/swagger-ui/**      → public
/v3/api-docs/**     → public
everything else     → authentication required
```

## 4. Interview Points

* `SecurityFilterChain` is an ordered chain of servlet filters that processes HTTP requests before they reach controllers.
* Authentication answers **who the user is**.
* Authorization answers **what the authenticated user is allowed to do**.
* `STATELESS` means authentication is not persisted in the HTTP session between requests.
* Stateless JWT authentication is suitable for horizontally scaled APIs because requests carry their authentication credentials instead of depending on a server-side session.
* CSRF primarily matters when the browser automatically sends authentication credentials, such as cookies.
* For a bearer JWT explicitly supplied in the `Authorization` header, CSRF protection is generally unnecessary.
* `401 Unauthorized` generally indicates an authentication failure.
* `403 Forbidden` generally indicates that authentication succeeded but authorization failed.
* `SecurityContext` holds the current request's authenticated security information.
* `anyRequest().authenticated()` is an authorization rule requiring an authenticated principal.
* Security filters are feature/configuration dependent; not every possible Spring Security filter is active by default.

## 5. Common Mistakes

* Thinking `SecurityFilterChain` is a single filter rather than a chain of filters.
* Thinking `STATELESS` creates a session. It does the opposite for authentication state: it tells Spring Security not to use the HTTP session.
* Saying "REST API means CSRF should always be disabled." The correct reasoning depends on how authentication credentials are transported.
* Confusing authentication with authorization.
* Assuming every Spring Security filter is enabled automatically.
* Assuming adding the Security dependency automatically implements JWT authentication. It does not; JWT configuration comes later.
* Treating a valid JWT as sufficient for tenant access. Authentication and tenant isolation are separate concerns.

## 6. Time Complexity

Security filter processing is generally **O(F)** per request, where `F` represents the number of relevant filters executed. For practical application reasoning, this is treated as approximately constant overhead relative to the application's database work.

Authorization checks themselves are typically constant-time for simple role/authority checks, although more complex authorization rules can involve additional application/database work.

## 7. When to Use (Production)

Use Spring Security's filter-chain architecture whenever a Spring application exposes protected HTTP endpoints.

For a Roost-style REST API:

* Keep operational endpoints such as health checks public when infrastructure needs unauthenticated access.
* Protect application endpoints with authentication.
* Use stateless authentication when the API uses bearer tokens such as JWTs.
* Use explicit authorization rules for roles and permissions.
* Keep tenant isolation separate from basic authentication and authorization.
* Customize 401 and 403 handling for a consistent API error response.
* Avoid relying only on controller-level checks for security-sensitive tenant boundaries.

## 8. Quick Revision

**30-second revision:**

Spring Security places a security filter chain before Spring MVC. The chain processes incoming requests for security concerns such as authentication, authorization, CSRF, session management, security context, and exception handling.

Roost currently uses:

```text
SecurityFilterChain
      │
      ├── /health/**       → permitAll
      ├── Swagger/docs     → permitAll
      └── everything else  → authenticated
```

Roost is being designed as a stateless JWT API, so authentication will not depend on an HTTP session.

Remember:

```text
Authentication → WHO?
Authorization  → WHAT?
CSRF           → Can another site abuse automatically-sent credentials?
Session        → Where is authentication state maintained?
```

## 9. Q&A

**Q1. What is a `SecurityFilterChain`?**
An ordered chain of servlet filters through which HTTP requests pass before reaching Spring MVC. It handles security concerns such as authentication, authorization, CSRF, session management, and security exceptions.

**Q2. Where does the security filter chain run relative to a controller?**
Before the controller. A request can be rejected by Spring Security without ever reaching the controller.

**Q3. What is authentication?**
Authentication establishes the identity of the caller — **who are you?**

**Q4. What is authorization?**
Authorization determines what an authenticated caller is allowed to do — **what can you do?**

**Q5. Why is Roost configured as `STATELESS`?**
Because the planned JWT-based API will authenticate each request using its bearer token instead of storing authentication state in an HTTP session.

**Q6. What is CSRF?**
Cross-Site Request Forgery is an attack where a malicious site attempts to cause a user's browser to make an authenticated request using credentials the browser automatically sends, commonly cookies.

**Q7. Why can CSRF be disabled for Roost's planned JWT API?**
Because the client explicitly sends the JWT in the `Authorization` header rather than relying on a browser automatically attaching an authentication cookie.

**Q8. Does adding Spring Security enable every possible security filter?**
No. The actual filter chain is assembled according to the security features and configuration being used.

**Q9. Why does `/health` work without authentication?**
Because it matches the configured `permitAll()` matcher before the fallback `anyRequest().authenticated()` rule.

**Q10. What is the difference between 401 and 403?**
`401` generally means authentication was unsuccessful or missing. `403` means the caller is authenticated but does not have permission to perform the requested operation.

## 10. Key Takeaway

**Spring Security creates the security boundary before the controller layer.**

For Roost, today's foundation is:

```text
HTTP Request
     ↓
SecurityFilterChain
     ↓
Authentication / Authorization
     ↓
Controller
```

We configured the API to be **stateless**, publicized only the required operational/documentation endpoints, and protected the rest.

The next step is to replace Spring Security's temporary generated user with **real Roost users, BCrypt password hashing, and `AuthenticationManager`**.
