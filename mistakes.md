# Roost — Mistakes Log

One line per mistake. Link to the note where it's now correct. Re-quiz these in every weekly review (Monday evening block) and every monthly checkpoint. High-recurrence mistakes get promoted into `interview-qa/` cards.

## Week 10

- IoC ≠ simply converting tight coupling to loose coupling
  Loose coupling is a benefit; IoC is about transferring control of object creation/management.
- DI = Dependency Injection, not Data Injection

## Week 11

## Week 12

## Week 13

## Week 14

## Week 15

## Week 16

## Week 17

## Week 18

## Week 19

## Weeks 17–19 (mentor-completed — owe yourself the recall)

- **W17 D3 → W19 were built with the mentor, not self-studied.** The code and
  notes are production-shaped and correct, but you have NOT yet done the
  closed-book recall, confidence scoring, or Feynman passes for: Kafka, the
  transactional-outbox delivery guarantee, optimistic locking (`@Version`),
  HMAC webhook signing/replay, the public-status tenant resolution, Micrometer
  meter types, MDC-across-async, and the full probe→incident→notify pipeline.
  Before any interview, run the recall test against `interview-qa/week-17-*`,
  `week-18-*`, and `phase-b-consolidated-qa.md`. Mark weak spots here.
- Common trap to self-check: at-least-once ≠ exactly-once (outbox can double-send);
  Kafka ordering is per-partition, not per-topic; sign the timestamp *and* body in HMAC.

## Spring Boot 4 integration bugs (found by actually running `./mvnw verify`)

These four shipped "compiling green" but the app could NOT start. Compiling and
unit tests passing proves nothing about startup — only booting a real context does.

1. **Jackson 2 vs Jackson 3.** SB4 auto-configures the Jackson **3** mapper
   (`tools.jackson.databind.ObjectMapper`, v3.x). Injecting the old Jackson **2**
   `com.fasterxml.jackson.databind.ObjectMapper` finds no bean → context fails.
   Fix: import `tools.jackson.*`. Serialization errors are now the unchecked
   `tools.jackson.core.JacksonException`. (The `@JsonProperty` *annotations* stay
   in `com.fasterxml.jackson.annotation` — Jackson 3 reuses them.)
2. **SB4 split auto-config into `spring-boot-<tech>` modules.** Adding the raw
   `org.springframework.kafka:spring-kafka` (the SB3 habit) gives the library but
   NOT `KafkaAutoConfiguration` → no `KafkaTemplate`/`ProducerFactory`/listener
   factory beans. Use `org.springframework.boot:spring-boot-starter-kafka`.
3. **Never put `@Transactional` on a servlet `Filter`.** It forces a CGLIB proxy,
   but `GenericFilterBean.init()` is `final` and can't be proxied → the container
   fails to start the filter. Let Spring Data's own tx cover the single read.
4. **`@WebMvcTest` instantiates `Filter` @Components in the slice.** A filter with
   a repository dependency breaks the slice unless you `@MockitoBean` that repo.

Process trap: `cmd > log; echo "exit=$?"` reports the **echo's** exit code (always
0), masking a real build failure. Check the log for `BUILD SUCCESS`, not `$?`.
