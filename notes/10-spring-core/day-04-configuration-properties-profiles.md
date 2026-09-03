# Configuration Properties & Profiles

**Week 1 · Day 4** · [DAILY-PLAN.md](../../DAILY-PLAN.md) entry link

## 1. Definition & Why

Spring Boot configuration allows application behavior to be controlled through external properties rather than hard-coding values. `@ConfigurationProperties` binds a related configuration block into a type-safe Java object, while Spring Profiles allow different configuration values for different environments such as development and production. This keeps configuration separate from application code and makes the same application easier to run across environments.

## 2. Mental Model

Think of configuration as:

```text
application.yml
      │
      │ base configuration
      ▼
application-dev.yml / application-prod.yml
      │
      │ profile overrides
      ▼
Spring Environment
      │
      │ binding
      ▼
@ConfigurationProperties
      │
      ▼
ProbeProperties
      │
      ▼
ProbeRunner
```

Example:

```text
application.yml
interval = 60
timeout  = 5000

        + dev profile
        ↓
interval = 30
timeout  = 5000

        + prod profile
        ↓
interval = 300
timeout  = 5000
```

The important distinction:

> **Profiles determine which configuration is active/overridden; `@ConfigurationProperties` binds the effective configuration into a typed Java object.**

## 3. My Roost Code

### Configuration

`src/main/resources/application.yml`

`src/main/resources/application-dev.yml`

`src/main/resources/application-prod.yml`

Base configuration:

```yaml
roost:
  probe:
    default-interval-seconds: 60
    default-timeout-millis: 5000
```

Dev overrides the interval to `30`, while prod overrides it to `300`.

### Configuration Properties

`src/main/java/com/chandu/roost/config/ProbeProperties.java`

```java
@ConfigurationProperties(prefix = "roost.probe")
public record ProbeProperties(
    int defaultIntervalSeconds,
    int defaultTimeoutMillis
) {}
```

The record provides an immutable, type-safe representation of the configuration.

### Registration

`src/main/java/com/chandu/roost/RoostApplication.java`

```java
@SpringBootApplication
@EnableConfigurationProperties(ProbeProperties.class)
public class RoostApplication {
    // Spring Boot entry point
}
```

`@EnableConfigurationProperties` registers `ProbeProperties` so Spring can create and bind it.

### Injection and Logging

`src/main/java/com/chandu/roost/iocdemo/ProbeRunner.java`

```java
private static final Logger log =
        LoggerFactory.getLogger(ProbeRunner.class);

private final ProbeProperties probeProperties;

@PostConstruct
public void init() {
    log.info("ProbeRunner initialized with properties: {}",
            probeProperties);
}
```

The logger is used instead of `System.out.println` so the application uses Spring Boot's normal logging infrastructure.

## 4. Interview Points

- **`@Value` vs `@ConfigurationProperties`**
  - `@Value` is suitable for a small number of individual properties.
  - `@ConfigurationProperties` is better for related configuration groups.
  - `@ConfigurationProperties` provides type-safe binding, validation support, and better IDE assistance.

- **Relaxed Binding**
  - Spring Boot allows supported naming conventions to bind to the same Java property.
  - Example:
    `roost.probe.default-interval-seconds`
    → `defaultIntervalSeconds`

- **Profiles**
  - `application.yml` contains base configuration.
  - `application-dev.yml` and `application-prod.yml` provide profile-specific overrides.
  - Only properties defined in the profile file are overridden; other base properties remain active.

- **Environment Variables**
  - Environment variables conventionally use uppercase letters and underscores.
  - `ROOST_PROBE_DEFAULT_INTERVAL_SECONDS=10` can bind to:
    `roost.probe.default-interval-seconds=10`.

- **Production Secrets**
  - Do not commit passwords, API keys, tokens, or other sensitive values to `application-prod.yml`.
  - Production secrets should come from external secret-management mechanisms or runtime environment configuration.

- **Configuration Binding Flow**
  - YAML/environment configuration → Spring Environment → `@ConfigurationProperties` → Java configuration object → injected into application components.

## 5. Common Mistakes

- **Using the wrong configuration prefix** — `@ConfigurationProperties(prefix = "roost.probe")` must correspond to the YAML structure under `roost.probe`.
- **Confusing profile configuration with `@ConfigurationProperties`** — profiles select/override configuration; `@ConfigurationProperties` performs the binding into Java.
- **Assuming every profile property must be repeated** — profile files only need to contain values that should differ from the base configuration.
- **Committing production secrets** — production credentials should not be stored in Git.

## 6. Quick Revision + Q&A

**Revision (30 sec):**

Spring Boot configuration externalizes application settings into files, environment variables, and other configuration sources. Profiles allow environment-specific overrides, while `@ConfigurationProperties` binds a structured configuration group into a type-safe Java object. The final effective configuration can therefore change by environment without changing application code.

**Q&A**

**Q1.** What is the difference between `@Value` and `@ConfigurationProperties`?

**A.** `@Value` is useful for individual properties, while `@ConfigurationProperties` is better for a related group of properties because it provides type-safe binding, validation support, and IDE assistance.

**Q2.** What is relaxed binding?

**A.** Relaxed binding allows configuration properties written using supported naming conventions to bind to the same Java property.

For example:

```text
roost.probe.default-interval-seconds
ROOST_PROBE_DEFAULT_INTERVAL_SECONDS
```

can bind to:

```java
defaultIntervalSeconds
```

**Q3.** How does `application-dev.yml` override `application.yml`?

**A.** When the `dev` profile is active, Spring Boot loads the profile-specific configuration and its values override corresponding values from the base configuration.

**Q4.** What happens to `default-timeout-millis` when the dev profile is active if only the interval is specified in `application-dev.yml`?

**A.** The timeout remains `5000` from the base `application.yml`.

**Q5.** How would Kubernetes provide `defaultIntervalSeconds = 10` through an environment variable?

**A.** Use:

```text
ROOST_PROBE_DEFAULT_INTERVAL_SECONDS=10
```

Spring Boot's relaxed binding maps that environment variable to the corresponding configuration property.

**Q6.** Does Spring literally convert every underscore into a hyphen?

**A.** No. Environment variables follow an uppercase/underscore naming convention, and Spring Boot's relaxed binding rules match that form to the canonical configuration property.

**Q7.** Why is `@ConfigurationProperties` useful for a configuration group?

**A.** It keeps related settings together in one typed object instead of scattering multiple `@Value` fields throughout the application.

**Q8.** Why is a Java record a good fit for `ProbeProperties`?

**A.** Configuration is naturally represented as immutable data. A record provides concise declaration, final components, and generated accessors/constructor.

**Q9.** What does `@EnableConfigurationProperties(ProbeProperties.class)` do?

**A.** It registers the `ProbeProperties` configuration-properties class so Spring Boot can create and bind it from the application's configuration.

**Q10.** Where should production secrets come from?

**A.** Prefer runtime-injected secrets from mechanisms such as Kubernetes Secrets, AWS Secrets Manager, Vault, or another managed secret store rather than committing secrets to source control.

**Q11.** What is the difference between a profile and a configuration property?

**A.** A profile represents an environment/configuration set such as `dev` or `prod`. A property is an individual configuration value within that environment.

**Q12.** What happens if no profile is active?

**A.** Spring Boot uses the base configuration from `application.yml` and does not apply profile-specific files such as `application-dev.yml`.

**Q13.** How can you activate the dev profile from the command line?

**A.**

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**Q14.** How can you activate the prod profile?

**A.**

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

**Q15.** What should the `ProbeRunner` log show with no profile active?

**A.**

```text
ProbeProperties[defaultIntervalSeconds=60, defaultTimeoutMillis=5000]
```

**Q16.** What should it show with the dev profile?

**A.**

```text
ProbeProperties[defaultIntervalSeconds=30, defaultTimeoutMillis=5000]
```

**Q17.** What should it show with the prod profile?

**A.**

```text
ProbeProperties[defaultIntervalSeconds=300, defaultTimeoutMillis=5000]
```

**Q18.** Why use a logger instead of `System.out.println` in a Spring Boot application?

**A.** A logger provides log levels, structured configuration, timestamps, output routing, filtering, and integration with production logging systems.

## Today's Key Takeaway

> **Configuration belongs outside application logic; Profiles control environment-specific values, and `@ConfigurationProperties` gives those values a type-safe Java representation.**
