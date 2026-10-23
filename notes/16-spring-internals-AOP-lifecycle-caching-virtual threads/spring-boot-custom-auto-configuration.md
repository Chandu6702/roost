# Spring Boot Custom Auto-Configuration

**Week 16 · Day 3** · [DAILY-PLAN](../../DAILY-PLAN.md)

## Definition & Why

Spring Boot auto-configuration provides beans automatically when specified conditions are satisfied. It reduces manual configuration while allowing applications to override default behavior.

In Roost, I implemented custom auto-configuration for `WebhookService`. Spring Boot discovers the configuration, binds webhook properties, and registers the service only when the required conditions match.

## Mental Model

```text
AutoConfiguration.imports
          ↓
Discover WebhookAutoConfiguration
          ↓
Bind WebhookProperties
          ↓
Check roost.webhook.enabled
          ↓
Check whether WebhookService exists
          ↓
Register the default bean if conditions match
```

- `@AutoConfiguration` defines the configuration class.
- `@EnableConfigurationProperties` enables typed configuration binding.
- `@ConditionalOnProperty` controls whether the configuration applies.
- `@ConditionalOnMissingBean` allows a custom bean to replace the default.
- `@Bean` registers the service in the Spring application context.

## My Roost Code

**1. Auto-configuration**

Path: `src/main/java/com/chandu/roost/webhook/WebhookAutoConfiguration.java`

- Uses `@AutoConfiguration`.
- Enables `WebhookProperties`.
- Uses `@ConditionalOnProperty` with `roost.webhook.enabled=true`.
- Declares a `WebhookService` bean using `@Bean`.
- Uses `@ConditionalOnMissingBean(WebhookService.class)` to provide a replaceable default.

**2. Configuration properties**

Path: `src/main/java/com/chandu/roost/webhook/WebhookProperties.java`

A Java record binds the following properties under `roost.webhook`:

- `enabled`
- `endpointUrl`
- `timeoutMs`

**3. Service**

Path: `src/main/java/com/chandu/roost/service/WebhookService.java`

- Receives `WebhookProperties` through constructor injection.
- Exposes the configured endpoint URL and timeout.
- Does not need `@Service` because the auto-configuration registers it through a conditional `@Bean` method.

**4. Auto-configuration discovery**

Path: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

Contains the fully qualified configuration class name:

`com.chandu.roost.webhook.WebhookAutoConfiguration`

This metadata allows Spring Boot to discover the custom auto-configuration.

**5. Application configuration**

Path: `src/main/resources/application.yml`

Relevant properties:

- `roost.webhook.enabled: true`
- `roost.webhook.endpoint-url: https://example.com/webhooks`
- `roost.webhook.timeout-ms: 5000`

**Verification completed:** Confirmed auto-configuration discovery and successful registration of `WebhookService` when enabled. Verified that the configured endpoint and timeout were bound correctly. Setting `roost.webhook.enabled=false` skipped registration through this auto-configuration.

## Interview Points

1. **What is Spring Boot auto-configuration?**  
   It automatically configures beans when applicable conditions are satisfied.

2. **What does `@AutoConfiguration` do?**  
   It marks a class as Spring Boot auto-configuration.

3. **Why is `AutoConfiguration.imports` needed?**  
   It lists auto-configuration classes for Spring Boot to discover.

4. **What does `@EnableConfigurationProperties` do?**  
   It enables registration and binding of the specified configuration-properties type.

5. **What does `@ConditionalOnProperty` do?**  
   It applies configuration when a property matches the required condition.

6. **What does `@ConditionalOnMissingBean` do?**  
   It allows a default bean definition to back off when a matching bean already exists.

7. **Why doesn't `WebhookService` need `@Service`?**  
   The conditional `@Bean` method registers it in the application context.

8. **What happens when `roost.webhook.enabled=false`?**  
   The auto-configuration does not apply, so it does not register its default service.

9. **How can an application override a default auto-configured service?**  
   It can provide its own `WebhookService` bean, provided that bean is visible when the missing-bean condition is evaluated.

10. **Is `@AutoConfiguration` alone enough to guarantee discovery?**  
    No. The class must also be imported or discovered through an appropriate mechanism, such as `AutoConfiguration.imports`.

## Common Mistakes

- Assuming `@AutoConfiguration` automatically guarantees discovery.
- Forgetting to list the configuration class in `AutoConfiguration.imports`.
- Registering the same service unconditionally with `@Component` when demonstrating conditional default registration.
- Confusing a skipped bean definition with a bean that was created and then disabled.
- Forgetting that `@ConditionalOnMissingBean` depends on which beans are visible when the condition is evaluated.
- Injecting an optional bean as a required dependency when its absence is a valid configuration outcome.

## Quick Revision

```text
@AutoConfiguration
    → Defines the configuration

AutoConfiguration.imports
    → Enables discovery

@EnableConfigurationProperties
    → Enables typed property binding

@ConditionalOnProperty
    → Controls configuration activation

@ConditionalOnMissingBean
    → Allows custom beans to replace defaults

@Bean
    → Registers the service
```

## Q&A

**Q1. Why isn't `WebhookService` annotated with `@Service`?**

Because it is registered through the conditional `@Bean` method in `WebhookAutoConfiguration`.

**Q2. What happens when `roost.webhook.enabled=false`?**

The auto-configuration is skipped, so its default `WebhookService` bean is not registered.

**Q3. Why do we use `@ConditionalOnMissingBean(WebhookService.class)`?**

To provide a default implementation without preventing applications from supplying their own implementation.

**Q4. What is the purpose of `AutoConfiguration.imports`?**

It lists auto-configuration classes for Spring Boot discovery.

**Q5. What happens if another configuration already provides a `WebhookService` bean?**

The default bean method backs off if the existing bean is visible when the condition is evaluated.

## Key Takeaway

Custom auto-configuration is about **discovery, conditional bean registration, and replaceable defaults**. A well-designed auto-configuration provides useful default behavior while allowing applications to customize it.

This Roost implementation demonstrates the mechanism inside the application. A separately packaged, reusable Spring Boot starter would require additional library packaging and dependency configuration.
