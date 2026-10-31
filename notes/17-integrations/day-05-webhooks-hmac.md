# W17 D5 — Outbound webhooks with HMAC signatures + retry

## Definition & Why
A webhook is an outbound HTTP callback: when something happens in Roost (an incident opens), we POST the event to URLs the tenant registered. The problem: the receiver must trust it's really us and that nobody tampered with the body. **HMAC-SHA256** solves this — we sign `timestamp + "." + body` with a shared secret and send the hex digest as `X-Roost-Signature`. The receiver recomputes it; a match proves authenticity *and* integrity.

## Mental Model
A signature is a tamper-evident wax seal keyed to a shared secret. Only someone with the secret can produce a seal that matches the exact bytes. Including the timestamp in the sealed material turns it into a seal with a date stamp — the receiver rejects anything older than a few minutes, so a captured-and-replayed request is useless. Retry-with-backoff is "knock louder, but less often" — give the receiver room to recover without hammering it.

## My Roost Code (today)
- `webhook_subscriptions` table (V13) — per-workspace `url` + `secret` + `active`; partial index on active rows for the fan-out lookup.
- `WebhookService` (delivery) — looks up active subscribers for the workspace, signs `timestamp.body` with the subscription secret, POSTs with `X-Roost-Signature` + `X-Roost-Timestamp`. **4xx → give up** (receiver says the request is bad); **5xx / connection error → retry** with exponential backoff + jitter (`500ms·2^n`), up to `max-attempts`. Built with the same `JdkClientHttpRequestFactory` timeout pattern as the probe client.
- `NotificationConsumer` now calls `webhookService.deliver(workspaceId, rawMessage)` — delivering the *exact* bytes it received so the subscriber's recomputed HMAC matches ours. `workspaceId` is now carried in the incident event payload.
- `WebhookSubscriptionService` + `WebhookSubscriptionController` — `POST/GET/DELETE /webhooks`, tenant-scoped; the secret is write-only (never echoed back).
- `WebhookService` is still built by the W16 `WebhookAutoConfiguration` (enabled flag), injected into the consumer via `ObjectProvider` so the app runs with webhooks off.
- `WebhookSignatureTest` pins the signing against the **RFC 4231** known-answer vector.

## Interview Points
- **HMAC vs API key**: an API key is a static bearer credential — leaked once, impersonated forever. HMAC proves *this exact body* was signed; you can't replay it with a different body.
- **Why sign the timestamp**: enables replay rejection (drop requests older than N minutes).
- **Backoff with jitter** prevents a thundering herd of synchronized retries after an outage.
- **Return codes from a webhook endpoint**: 2xx = done; 4xx = don't retry (bad request); 5xx = retry.
- Deliver verbatim bytes — re-serializing JSON can reorder keys and break the signature.

## Common Mistakes
- Re-serializing the body before signing vs before sending → signatures won't match.
- Retrying on 4xx (wasted calls; the receiver already said "no").
- No cap on retries → a dead endpoint ties up a thread forever.
- Logging or returning the secret. It's write-only.
- Signing only the body (no timestamp) → open to replay.

## Quick Revision + Q&A
**Q: Why HMAC and not just HTTPS?**
A: HTTPS secures the channel but doesn't prove *who* sent the request to the app — a receiver behind a proxy/CDN can't tell Roost from an attacker who learned the URL. HMAC authenticates the sender at the application layer.

**Q: How does the receiver stop replays?**
A: Reject if `now - X-Roost-Timestamp` exceeds a few minutes; the timestamp is inside the signed material so it can't be forged.

**Q: Where does retry logic belong?**
A: In a wrapper around the transport (business policy), not inside the HTTP client. Or Spring Retry `@Retryable`.

**Q: What backoff did you pick and why?**
A: Exponential (`500ms·2^n`) + random jitter, capped at `max-attempts`. Exponential gives a struggling receiver increasing breathing room; jitter de-synchronizes retries across many events.
