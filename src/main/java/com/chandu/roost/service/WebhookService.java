package com.chandu.roost.service;

import com.chandu.roost.entity.WebhookSubscription;
import com.chandu.roost.repository.WebhookSubscriptionRepository;
import com.chandu.roost.webhook.WebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Delivers signed incident events to a tenant's registered webhook endpoints.
 *
 * <p>Each delivery carries an {@code X-Roost-Signature} = HMAC-SHA256 over
 * {@code "<timestamp>.<body>"} using the subscription's secret. Signing the
 * timestamp as well as the body lets subscribers reject replays (stale
 * timestamp) and tampering (body doesn't match the MAC). Failed deliveries
 * retry with exponential backoff + jitter and give up after {@code maxAttempts}.
 *
 * <p>Instantiated by {@code WebhookAutoConfiguration} only when
 * {@code roost.webhook.enabled=true} — the W16 custom-starter pattern.
 */
public class WebhookService {

    private static final Logger log =
            LoggerFactory.getLogger(WebhookService.class);

    private static final String HMAC_ALGO = "HmacSHA256";

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookProperties properties;
    private final RestClient restClient;

    public WebhookService(
            WebhookSubscriptionRepository subscriptionRepository,
            WebhookProperties properties) {

        this.subscriptionRepository = subscriptionRepository;
        this.properties = properties;
        this.restClient = RestClient.builder()
                .requestFactory(timeoutFactory(properties.timeoutMs()))
                .build();
    }

    private static JdkClientHttpRequestFactory timeoutFactory(long timeoutMs) {
        Duration timeout = Duration.ofMillis(timeoutMs);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return factory;
    }

    /**
     * Fan out one event body to every active subscriber of the workspace.
     * The body is delivered verbatim (byte-for-byte) so the signature the
     * subscriber recomputes matches ours.
     */
    public void deliver(Long workspaceId, String body) {
        List<WebhookSubscription> subscribers =
                subscriptionRepository.findByWorkspaceIdAndActiveTrue(workspaceId);

        if (subscribers.isEmpty()) {
            log.debug("No active webhook subscribers for workspace {}", workspaceId);
            return;
        }

        for (WebhookSubscription sub : subscribers) {
            deliverToSubscriber(sub, body);
        }
    }

    private void deliverToSubscriber(WebhookSubscription sub, String body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign(sub.getSecret(), timestamp + "." + body);

        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                var response = restClient.post()
                        .uri(sub.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Roost-Signature", signature)
                        .header("X-Roost-Timestamp", timestamp)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();

                log.info("Webhook delivered to subscription {} ({}) attempt {}",
                        sub.getId(), response.getStatusCode(), attempt);
                return;
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // 4xx: subscriber says the request is bad — do not retry.
                log.warn("Webhook subscription {} rejected delivery ({}), giving up",
                        sub.getId(), e.getStatusCode());
                return;
            } catch (Exception e) {
                // 5xx / connection errors: retry with backoff.
                log.warn("Webhook delivery to subscription {} failed on attempt {}/{}: {}",
                        sub.getId(), attempt, properties.maxAttempts(), e.getMessage());
                if (attempt < properties.maxAttempts() && !backoff(attempt)) {
                    return;
                }
            }
        }
        log.error("Webhook subscription {} exhausted {} attempts; dropping event",
                sub.getId(), properties.maxAttempts());
    }

    /** @return false if the wait was interrupted (caller should stop). */
    private boolean backoff(int attempt) {
        long base = 500L * (1L << (attempt - 1));   // 500ms, 1s, 2s, 4s, ...
        long jitter = ThreadLocalRandom.current().nextLong(250);
        try {
            Thread.sleep(base + jitter);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static String sign(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to sign webhook payload", e);
        }
    }
}
