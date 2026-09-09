# DTOs, Jackson, Records

**Week 11 · Day 2** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

DTOs (**Data Transfer Objects**) define the data exchanged between the client and the API, while the domain/entity model represents the application's internal state.

For Roost, `MonitorRequest` represents client input, `Monitor` represents the internal domain object, and `MonitorResponse` represents client output.

Separating DTOs from domain objects prevents internal fields such as audit timestamps, tenant IDs, versioning information, or database-specific details from leaking into the API contract. It also means database/domain changes do not automatically break the API.

Java **records** are a good fit for DTOs because DTOs are usually simple data carriers. Records provide compact syntax, immutability, and generated `equals()`, `hashCode()`, and `toString()` methods.

**Jackson** is Spring Boot's JSON library. It converts incoming JSON into Java objects (**deserialization**) and Java objects into outgoing JSON (**serialization**).

## 2. Mental Model

Think of the REST API as a boundary between the client and the application:

```text
Client
  │
  │ JSON request
  ▼
MonitorRequest          ← API input DTO
  │
  ▼
MonitorController
  │
  ▼
MonitorService
  │
  ▼
Monitor                 ← Domain object
  │
  ▼
MonitorController
  │
  ▼
MonitorResponse         ← API output DTO
  │
  │ JSON response
  ▼
Client
```

The important rule is:

```text
API contract ≠ Internal domain model
```

Jackson handles the JSON conversion:

```text
JSON → Java object     = Deserialization
Java object → JSON     = Serialization
```

For example, Java can keep:

```java
Long checkIntervalSeconds
```

while the API exposes:

```json
"check_interval_seconds": 60
```

using `@JsonProperty`.

## 3. My Roost Code

### DTOs

`src/main/java/com/chandu/roost/monitor/MonitorRequest.java`

`MonitorRequest` is a record containing only client-provided fields.

```java
public record MonitorRequest(
        String name,
        String url,
        String status,
        @JsonProperty("check_interval_seconds")
        Integer checkIntervalSeconds
) {}
```

`src/main/java/com/chandu/roost/monitor/MonitorResponse.java`

`MonitorResponse` is a separate record containing the fields exposed to the client.

```java
public record MonitorResponse(
        Long id,
        String name,
        String url,
        String status,
        @JsonProperty("check_interval_seconds")
        Integer checkIntervalSeconds
) {}
```

`src/main/java/com/chandu/roost/monitor/Monitor.java`

`Monitor` remains a normal class because it represents the internal domain object.

The service maps the request DTO into the domain object:

```java
Long id = nextId++;

Monitor newMonitor = new Monitor(
        id,
        request.name(),
        request.url(),
        request.status(),
        request.checkIntervalSeconds()
);
```

The controller then maps the domain object into the response DTO:

```java
MonitorResponse response = new MonitorResponse(
        monitor.getId(),
        monitor.getName(),
        monitor.getUrl(),
        monitor.getStatus(),
        monitor.getCheckIntervalSeconds()
);
```

### Jackson configuration

`src/main/resources/application.yml`

Unknown JSON properties were initially ignored by default. We changed the configuration so invalid/unknown request fields fail loudly:

```yaml
spring:
  jackson:
    deserialization:
      fail-on-unknown-properties: true
```

Testing with an unknown field such as `something_unknown` produced a `400 Bad Request` because Jackson could not deserialize the request body.

### API behavior

The Day 2 controller now uses:

```text
MonitorRequest   → POST/PUT input
Monitor          → internal domain object
MonitorResponse  → GET/POST/PUT output
```

All CRUD endpoints were tested successfully after fixing the POST response type.

## 4. Interview Points

- **Why DTOs?**
  - Prevent internal/domain fields from leaking through the API.
  - Decouple the API contract from the database/domain model.
  - Allow the database model and API model to evolve independently.
  - Avoid problems such as circular JSON relationships.

- **Why records for DTOs?**
  - Compact syntax.
  - Immutable data carriers.
  - Automatically provide `equals()`, `hashCode()`, and `toString()`.
  - Good fit for request/response objects.

- **`@JsonProperty`**
  - Maps a Java property to a specific JSON property name.
  - Example: `checkIntervalSeconds` ↔ `check_interval_seconds`.

- **`@JsonIgnore` vs `READ_ONLY`**
  - `@JsonIgnore`: property is ignored during both serialization and deserialization.
  - `@JsonProperty(access = JsonProperty.Access.READ_ONLY)`: property can be returned to the client but cannot be supplied by the client.
  - READ_ONLY is useful for server-owned fields such as `id`.

- **Unknown JSON properties**
  - By default, Jackson generally ignores unknown properties.
  - With `fail-on-unknown-properties: true`, Jackson throws an `UnrecognizedPropertyException`.
  - Spring then returns `400 Bad Request` because the request body could not be deserialized.

- **Jackson and records**
  - Jackson understands Java records and uses their canonical constructor to create instances.
  - `@JsonCreator` or parameter annotations can be used when explicit constructor/deserialization configuration is needed.

- **Layer responsibility**
  - Controller handles HTTP/API concerns.
  - Service handles application/domain logic.
  - Domain object represents internal state.
  - DTOs define the external API contract.

## 5. Common Mistakes

- **Using the entity/domain object directly as the API request and response**
  - Leaks internal state and couples the API to the domain/database model.

- **Returning `Monitor` when the controller should return `MonitorResponse`**
  - The API should expose the response DTO, not the internal domain object.

- **POST generic type mismatch**
  - The controller initially declared:
    `ResponseEntity<Monitor>`
  - but returned:
    `MonitorResponse`.
  - Correct type:
    `ResponseEntity<MonitorResponse>`.

- **Putting `ResponseEntity` inside the service**
  - The service should return domain/application data.
  - The controller should decide HTTP status, headers, and response body.

- **Confusing Java property names with JSON property names**
  - Java can remain camelCase while the JSON API uses snake_case through `@JsonProperty`.

- **Assuming unknown JSON fields always cause an error**
  - Jackson normally ignores them unless configured to fail on unknown properties.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

```text
Request JSON
    ↓
MonitorRequest (record)
    ↓
Controller
    ↓
MonitorService
    ↓
Monitor (domain object)
    ↓
Controller
    ↓
MonitorResponse (record)
    ↓
Response JSON
```

**Key idea:** DTOs protect the API contract from the internal domain model.

**Q&A**

**Q1. Why shouldn't we use `Monitor` directly for both database/domain and API communication?**
Because internal fields can leak into the API, the API becomes coupled to the domain/database structure, and internal changes can unintentionally break clients.

**Q2. Why are records good for DTOs?**
They provide compact immutable data carriers with generated `equals()`, `hashCode()`, and `toString()`.

**Q3. What is the difference between `@JsonIgnore` and READ_ONLY?**
`@JsonIgnore` excludes the property from both serialization and deserialization. READ_ONLY allows serialization but prevents the client from setting the property during deserialization.

**Q4. What happens when `fail-on-unknown-properties` is enabled and the client sends an unknown field?**
Jackson throws `UnrecognizedPropertyException`, request-body deserialization fails, and Spring returns `400 Bad Request`.

**Q5. How does Jackson construct a Java record?**
Jackson uses the record's canonical constructor and maps JSON properties to its constructor components.

**Q6. Why use `@JsonProperty("check_interval_seconds")`?**
It allows the Java property to remain `checkIntervalSeconds` while exposing `check_interval_seconds` in the JSON API.

**Q7. What should the controller return: `Monitor` or `MonitorResponse`?**
`MonitorResponse`, because the controller is responsible for exposing the API contract rather than leaking the internal domain object.
