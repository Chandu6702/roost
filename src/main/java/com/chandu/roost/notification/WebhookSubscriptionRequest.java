package com.chandu.roost.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record WebhookSubscriptionRequest(

        @NotBlank
        @URL
        String url,

        @NotBlank
        @Size(min = 16, max = 255, message = "secret must be 16-255 characters")
        String secret
) {
}
