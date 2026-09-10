# Bean Validation on MonitorRequest

**Week 11 · Day 3** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

**Bean Validation** is a standard way to declare constraints on Java objects using annotations such as `@NotBlank`, `@NotNull`, `@Min`, `@Max`, and `@URL`. In Spring Boot, adding `@Valid` to a `@RequestBody` tells Spring to validate the incoming DTO before the controller method executes.

For Roost, validation belongs on `MonitorRequest` because it is the API input boundary. This prevents invalid monitor data from entering the application and keeps basic input validation separate from the internal `Monitor` domain object.

## 2. Mental Model

Think of validation as a gate at the API boundary:

```text
Client
  │
  │ JSON
  ▼
MonitorRequest
  │
  │ @Valid
  ▼
Bean Validation
  │
  ├── Valid ──────→ Controller method executes
  │
  └── Invalid ────→ Validation fails
                         │
                         ▼
                  400 Bad Request
```

The important point is that validation happens **before the controller handler executes**.

Common constraints:

```text
@NotNull   → value cannot be null
@NotEmpty  → cannot be null or empty
@NotBlank  → String must contain non-whitespace characters

@Min(30)   → value must be >= 30
@Max(3600) → value must be <= 3600
@URL       → value must be a valid URL
```

For nested objects, `@Valid` enables **cascaded validation** into the nested object.

## 3. My Roost Code

`src/main/java/com/chandu/roost/monitor/MonitorRequest.java`

Validation constraints were added directly to the request DTO:

```java
public record MonitorRequest(
        @NotBlank String name,

        @NotBlank
        @URL
        String url,

        String status,

        @Min(30)
        @Max(3600)
        Integer checkIntervalSeconds,

        @NotNull
        MonitorType type
) {}
```

`src/main/java/com/chandu/roost/monitor/MonitorType.java`

A `MonitorType` enum was added so the request accepts a defined set of monitor types rather than an arbitrary string.

`src/main/java/com/chandu/roost/monitor/MonitorController.java`

`@Valid` was added to the request body for endpoints receiving `MonitorRequest`:

```java
public ResponseEntity<MonitorResponse> createMonitor(
        @Valid @RequestBody MonitorRequest request) {
    // ...
}
```

The same validation is applied to the PUT request.

Invalid requests were tested for:

* missing/blank `name`
* invalid `url`
* interval below `30`
* interval above `3600`
* missing `type`

These requests correctly returned **400 Bad Request**.

The raw validation response is intentionally left ugly for now; error handling will be improved in the following day.

## 4. Interview Points

* **What does `@Valid` do?**

  * Triggers Bean Validation for the request object.
  * Validation occurs during Spring MVC argument resolution before the controller method executes.

* **`@NotNull` vs `@NotEmpty` vs `@NotBlank`**

  * `@NotNull`: rejects only `null`.
  * `@NotEmpty`: rejects `null` and empty values.
  * `@NotBlank`: for strings; rejects `null`, empty, and whitespace-only values.

* **Where should API input validation go?**

  * Primarily on DTOs because DTOs represent the API boundary.
  * Entities/domain objects can have their own domain invariants that must be enforced independently.

* **What happens when request validation fails?**

  * Spring raises `MethodArgumentNotValidException`.
  * The controller method body is not executed.
  * Spring MVC produces a `400 Bad Request` response.

* **How does nested validation work?**

  * Put `@Valid` on the nested object so Bean Validation cascades into it.

* **How do you create a custom constraint?**

  * Create a `@ValidSlug` annotation.
  * Mark it with `@Constraint(validatedBy = SlugValidator.class)`.
  * Implement `ConstraintValidator<ValidSlug, String>`.
  * Put the validation logic inside `isValid()`.

## 5. Common Mistakes

* **Using `@NotNull` when whitespace should also be rejected**

  * `@NotNull` allows `""` and `"   "`.
  * Use `@NotBlank` for required text input.

* **Forgetting `@Valid` on `@RequestBody`**

  * Adding validation annotations to the DTO alone does not cause Spring MVC request validation to run.

* **Putting API input validation only on the entity**

  * DTO validation belongs at the API boundary.
  * Domain objects should separately enforce domain-level invariants where required.

* **Assuming validation happens inside the controller method**

  * `MethodArgumentNotValidException` occurs during argument resolution, before the handler method executes.

* **Forgetting `@Valid` for nested DTOs**

  * Without cascaded validation, constraints inside the nested object are not automatically validated.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

```text
JSON
 ↓
@RequestBody
 ↓
@Valid
 ↓
Bean Validation
 ↓
 ├── Valid   → Controller executes
 └── Invalid → MethodArgumentNotValidException → 400
```

Validation belongs primarily at the **DTO/API boundary**.

**Q&A**

**Q1. What is the difference between `@NotNull`, `@NotEmpty`, and `@NotBlank`?**
`@NotNull` rejects `null`. `@NotEmpty` rejects `null` and empty values. `@NotBlank` is for strings and rejects `null`, empty, and whitespace-only values.

**Q2. Where should API input validation constraints normally be placed?**
On the DTO because validation is part of the API boundary. Entities/domain objects can have their own stricter domain invariants.

**Q3. What exception is thrown when `@Valid` fails on a `@RequestBody`?**
`MethodArgumentNotValidException`.

**Q4. When does that exception occur?**
During Spring MVC argument resolution, before the controller handler method executes.

**Q5. How do you validate a nested DTO?**
Put `@Valid` on the nested field, for example `@Valid NotificationChannelRequest notification`.

**Q6. Why do we use `@Min(30)` and `@Max(3600)` for the monitor interval?**
They restrict the accepted interval to the application's defined range of 30 through 3600 seconds.

**Q7. How would you create a custom `@ValidSlug` constraint?**
Create the annotation, connect it using `@Constraint(validatedBy = SlugValidator.class)`, and implement the validation logic in a `ConstraintValidator<ValidSlug, String>`.

**Q8. What is the role of `@URL` on `MonitorRequest.url`?**
It validates that the supplied URL has a valid URL format before the request reaches the controller logic.
