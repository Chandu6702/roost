# Authentication, JWT & Role-Based Authorization

**Week 15 · Days 2–4** · [DAILY-PLAN.md](../../DAILY-PLAN.md)

## 1. Definition & Why

Authentication verifies **who the user is**; authorization determines **what the authenticated user is allowed to do**.

In Roost, authentication starts with a database-backed user account. Passwords are stored using BCrypt rather than plaintext. After successful login, Roost issues a short-lived JWT. The client then sends that JWT with subsequent API requests. Spring Security validates the token and converts its `role` claim into Spring Security authorities. Method-level authorization then decides whether the authenticated user can access a protected operation.

This gives Roost a **stateless authentication model** and the foundation for role-based access control.

---

## 2. Mental Model

### Authentication

```text
REGISTER
email + password
        ↓
PasswordEncoder / BCrypt
        ↓
password_hash stored in PostgreSQL
```

```text
LOGIN
email + password
        ↓
AuthenticationManager
        ↓
UserDetailsService
        ↓
UserRepository
        ↓
PostgreSQL
        ↓
PasswordEncoder verifies password
        ↓
JwtService
        ↓
signed JWT
```

### Every protected request

```text
Authorization: Bearer <JWT>
        ↓
Spring Security Resource Server
        ↓
JwtDecoder
        ↓
JWT signature + claims validated
        ↓
JwtAuthenticationConverter
        ↓
role → ROLE_*
        ↓
@PreAuthorize
        ↓
ALLOW / 403
```

### Authentication vs Authorization

```text
Authentication
→ "Who are you?"

Authorization
→ "What are you allowed to do?"
```

### 401 vs 403

```text
401 Unauthorized
→ authentication failed or credentials are missing/invalid

403 Forbidden
→ authentication succeeded, but permission is insufficient
```

---

## 3. My Roost Code

### Day 2 — Users, password hashing & login

Relevant files:

```text
src/main/resources/db/migration/V9__users.sql

src/main/java/com/chandu/roost/entity/User.java
src/main/java/com/chandu/roost/repository/UserRepository.java
src/main/java/com/chandu/roost/security/RoostUserDetailsService.java

src/main/java/com/chandu/roost/dto/RegisterRequest.java
src/main/java/com/chandu/roost/dto/LoginRequest.java
src/main/java/com/chandu/roost/controller/AuthController.java

src/main/java/com/chandu/roost/config/SecurityConfig.java
```

### Database

`V9__users.sql`

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(30) NOT NULL
);
```

The database stores the **BCrypt hash**, not the user's original password.

### Registration

`AuthController.java`

```java
User user = new User(
        request.email(),
        passwordEncoder.encode(request.password()),
        "MEMBER"
);

userRepository.save(user);
```

Important:

* Password is encoded before persistence.
* The client does not choose the role.
* New users are currently assigned `MEMBER`.

### User lookup

`UserRepository.java`

```java
Optional<User> findByEmail(String email);
```

`RoostUserDetailsService` adapts the Roost `User` entity to Spring Security's `UserDetails` model.

```text
Spring Security
      ↓
UserDetailsService
      ↓
UserRepository
      ↓
User entity
```

### AuthenticationManager

`SecurityConfig.java`

```java
DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider(userDetailsService);

provider.setPasswordEncoder(passwordEncoder);

return new ProviderManager(provider);
```

Responsibilities:

* `AuthenticationManager` orchestrates authentication.
* `UserDetailsService` loads the user.
* `PasswordEncoder` verifies the password.

---

### Day 3 — JWT generation & validation

Relevant files:

```text
src/main/java/com/chandu/roost/security/JwtService.java
src/main/java/com/chandu/roost/dto/LoginResponse.java
src/main/java/com/chandu/roost/controller/AuthController.java
src/main/java/com/chandu/roost/config/SecurityConfig.java

src/main/resources/application-local.yml
```

The JWT secret is kept in local untracked configuration:

```yaml
jwt:
  secret: roost-dev-secret-key-change-this-to-a-long-random-value-1234567890
```

`application-local.yml` is excluded from Git.

### JWT claims

`JwtService.java`

```java
JwtClaimsSet claims = JwtClaimsSet.builder()
        .subject(user.getEmail())
        .claim("role", user.getRole())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(15 * 60))
        .build();
```

Roost's JWT currently contains:

```text
sub  → user email
role → MEMBER / ADMIN
iat  → issued-at time
exp  → expiration time
```

The token uses **HS256**.

### Login flow

```text
POST /auth/login
        ↓
AuthenticationManager.authenticate(...)
        ↓
credentials verified
        ↓
User loaded from database
        ↓
JwtService.issueToken(user)
        ↓
LoginResponse(token)
```

### JWT validation

`SecurityConfig.java`

```java
.oauth2ResourceServer(oauth ->
        oauth.jwt(jwt ->
                jwt.jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                )
        )
)
```

This tells Spring Security to treat incoming requests as bearer-JWT requests.

---

### Day 4 — Role-based authorization

Relevant files:

```text
src/main/java/com/chandu/roost/config/SecurityConfig.java
src/main/java/com/chandu/roost/controller/AuthController.java
src/main/java/com/chandu/roost/error/GlobalExceptionHandler.java
```

Method security is enabled:

```java
@EnableMethodSecurity
```

The JWT's custom `role` claim is mapped to Spring Security authorities:

```java
authoritiesConverter.setAuthoritiesClaimName("role");
authoritiesConverter.setAuthorityPrefix("ROLE_");
```

Therefore:

```text
JWT:
"role": "MEMBER"

        ↓

Spring Security:
ROLE_MEMBER
```

and:

```text
JWT:
"role": "ADMIN"

        ↓

Spring Security:
ROLE_ADMIN
```

### Protected endpoint

`AuthController.java`

```java
@GetMapping("/admin-test")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<String> adminTest() {
    return ResponseEntity.ok("ADMIN access granted");
}
```

### Final behavior tested

```text
MEMBER JWT
    ↓
@PreAuthorize("hasRole('ADMIN')")
    ↓
403 Forbidden
```

```text
ADMIN JWT
    ↓
@PreAuthorize("hasRole('ADMIN')")
    ↓
200 OK
```

### Error handling

`GlobalExceptionHandler.java` maps `AccessDeniedException` to HTTP 403 with a `ProblemDetail`.

This keeps authorization failures consistent with Roost's centralized API error format.

---

## 4. Interview Points

* **Authentication** establishes identity.
* **Authorization** determines access.
* BCrypt stores passwords as one-way password hashes.
* `AuthenticationManager` coordinates the authentication process.
* `UserDetailsService` loads user information for Spring Security.
* `PasswordEncoder` verifies the supplied password against the stored hash.
* JWT provides stateless authentication between requests.
* JWT is **signed, not encrypted**.
* `JwtEncoder` creates/signs the token.
* `JwtDecoder` validates incoming JWTs.
* `JwtClaimsSet` represents JWT claims.
* `JwsHeader` contains signing-related information such as the algorithm.
* `iat` means issued-at.
* `exp` means expiration.
* `JwtAuthenticationConverter` converts JWT claims into Spring Security authorities.
* `hasRole("ADMIN")` checks for the `ROLE_ADMIN` authority.
* `401` means authentication failed or is missing.
* `403` means authentication succeeded but authorization failed.
* A database role change does not modify an already-issued JWT.
* A new JWT is required after changing the user's role.
* Stateless security avoids relying on an HTTP session for authentication state.

---

## 5. Common Mistakes

* Storing plaintext passwords instead of BCrypt hashes.
* Allowing the client to provide its own role during registration.
* Assuming a custom JWT `role` claim automatically becomes a Spring Security authority.
* Forgetting the `ROLE_` prefix when using `hasRole(...)`.
* Confusing `401 Unauthorized` with `403 Forbidden`.
* Expecting an already-issued JWT to reflect a later database role change.
* Committing the JWT signing secret to Git.
* Assuming JWT payload data is encrypted.
* Sending the user's password with every API request instead of using the issued token.
* Adding tenant/workspace authorization before designing the actual tenant relationship.

**Mistakes tracked:** No new genuine user mistake was added to `mistakes.md` for Days 2–4. The temporary 500 responses during authorization testing were configuration/error-handling issues discovered during implementation, not mistakes in the user's final authorization logic.

---

## 6. Time Complexity

### User lookup

```text
findByEmail(...)
```

With the unique database index on `email`, lookup is effectively **O(1)** average from the application's perspective.

### BCrypt

BCrypt password verification is intentionally computationally expensive.

It should not be thought of as an ordinary constant-time hash such as a simple lookup or checksum. Its work factor is deliberately configurable to make password cracking expensive.

### JWT generation / validation

JWT processing is approximately:

```text
O(token size)
```

For Roost's small JWT, this is effectively constant with respect to the application's database size.

### Authorization

Checking a small set of authorities is effectively:

```text
O(1)
```

for the normal Roost request.

---

## 7. When to Use (Production)

### Stateless JWT authentication is useful when:

* Building REST APIs.
* Supporting SPAs or mobile clients.
* Multiple backend services need bearer-token authentication.
* Server-side HTTP sessions are undesirable.
* Horizontal scaling should not depend on a shared session store.

### Production considerations

* Store signing secrets in a secret manager or protected environment configuration.
* Never commit production secrets to Git.
* Use strong randomly generated secrets.
* Keep access tokens relatively short-lived.
* Consider refresh tokens for long-lived login sessions.
* Validate appropriate claims such as expiration, issuer, and audience.
* Use asymmetric algorithms such as RS256 when multiple services need to verify tokens without sharing the private signing secret.
* Never trust a role supplied by the client.
* Authorization decisions must remain server-side.

---

## 8. Quick Revision

### Authentication

```text
Credentials
    ↓
AuthenticationManager
    ↓
UserDetailsService
    ↓
UserRepository
    ↓
PasswordEncoder
    ↓
Authenticated
```

### JWT

```text
Authenticated user
    ↓
JwtEncoder
    ↓
Signed JWT
    ↓
Client stores/sends token
```

### Protected request

```text
Bearer JWT
    ↓
JwtDecoder
    ↓
JWT validated
    ↓
JwtAuthenticationConverter
    ↓
ROLE_MEMBER / ROLE_ADMIN
    ↓
@PreAuthorize
```

### Status codes

```text
401 → "I don't know who you are."

403 → "I know who you are, but you cannot do this."
```

### JWT structure

```text
HEADER.PAYLOAD.SIGNATURE
```

```text
HEADER
→ algorithm information

PAYLOAD
→ claims such as sub, role, iat, exp

SIGNATURE
→ verifies the token was signed with the expected secret
```

---

## 9. Q&A

### Q1. Why do we need JWT after login?

**A:** Login verifies the credentials once. The JWT provides a reusable proof of authentication for subsequent stateless requests without sending the password again.

### Q2. What is the difference between `AuthenticationManager` and `UserDetailsService`?

**A:** `AuthenticationManager` orchestrates authentication. `UserDetailsService` loads the user's security information from the application's data source.

### Q3. What does `PasswordEncoder` do?

**A:** It hashes passwords during registration and verifies a supplied password against the stored hash during authentication.

### Q4. Is BCrypt encryption?

**A:** No. BCrypt is a password-hashing algorithm. Password hashes are not meant to be decrypted back into the original password.

### Q5. What does `JwtEncoder` do?

**A:** It creates and signs the JWT during successful login.

### Q6. What does `JwtDecoder` do?

**A:** It parses and validates incoming JWTs, including cryptographic signature validation and relevant token claims.

### Q7. What are `iat` and `exp`?

**A:** `iat` is the issued-at timestamp. `exp` is the expiration timestamp.

### Q8. Why did Roost add `JwtAuthenticationConverter`?

**A:** Roost uses a custom `role` claim. The converter tells Spring Security how to transform that claim into authorities such as `ROLE_MEMBER` and `ROLE_ADMIN`.

### Q9. Why does `hasRole("ADMIN")` work with `ROLE_ADMIN`?

**A:** Spring Security's `hasRole("ADMIN")` convention checks for the `ROLE_ADMIN` authority.

### Q10. What is the difference between 401 and 403?

**A:** `401` means the request is not successfully authenticated. `403` means the user is authenticated but does not have sufficient permission.

### Q11. What happens if an ADMIN is changed to MEMBER in the database while their old JWT is still valid?

**A:** The old JWT still contains its original `ADMIN` role claim. Unless the architecture performs server-side role revalidation or token revocation, the existing token remains authoritative until it expires.

### Q12. Why shouldn't JWT be used to store sensitive information?

**A:** JWT payloads are normally readable by whoever possesses the token. Signing protects integrity and authenticity; it does not provide confidentiality.

---

## 10. Key Takeaway

Roost now has the complete authentication and authorization pipeline:

```text
User credentials
      ↓
BCrypt authentication
      ↓
Short-lived JWT
      ↓
Bearer-token validation
      ↓
Role claim conversion
      ↓
Spring Security authority
      ↓
Method-level authorization
```

The most important interview distinction is:

> **Authentication answers "Who are you?"**
> **Authorization answers "What are you allowed to do?"**

Days 2–4 establish the security identity layer. The next step is to apply that identity to **workspace-level tenant isolation**, so that an authenticated user can access only the resources belonging to their own workspace.
