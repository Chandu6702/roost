package com.chandu.roost.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureTest {

    @Test
    void signatureIsDeterministicForSameInput() {
        String a = WebhookService.sign("supersecretkey123", "1700000000.{\"x\":1}");
        String b = WebhookService.sign("supersecretkey123", "1700000000.{\"x\":1}");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSize(64); // SHA-256 => 32 bytes => 64 hex chars
    }

    @Test
    void differentSecretProducesDifferentSignature() {
        String withKey1 = WebhookService.sign("key-one-sixteen!", "1700000000.body");
        String withKey2 = WebhookService.sign("key-two-sixteen!", "1700000000.body");

        assertThat(withKey1).isNotEqualTo(withKey2);
    }

    @Test
    void differentBodyProducesDifferentSignature() {
        String body1 = WebhookService.sign("supersecretkey123", "1700000000.body-a");
        String body2 = WebhookService.sign("supersecretkey123", "1700000000.body-b");

        assertThat(body1).isNotEqualTo(body2);
    }

    @Test
    void matchesKnownRfc4231Vector() {
        // RFC 4231 test case 1: key=0x0b*20, data="Hi There"
        String key = "\u000b".repeat(20);
        String sig = WebhookService.sign(key, "Hi There");

        assertThat(sig).isEqualTo(
                "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7");
    }
}
