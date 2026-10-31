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
