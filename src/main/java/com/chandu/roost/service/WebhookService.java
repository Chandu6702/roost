package com.chandu.roost.service;

import com.chandu.roost.webhook.WebhookProperties;

public class WebhookService {

    private final WebhookProperties webhookProperties;

    public WebhookService(WebhookProperties webhookProperties) {
        this.webhookProperties = webhookProperties;
    }

    public String getEndpointUrl() {
        return webhookProperties.endpointUrl();
    }

    public long getTimeoutMs() {
        return webhookProperties.timeoutMs();
    }
}
