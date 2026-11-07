package com.chandu.roost.notification;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(WebhookProperties.class)
@ConditionalOnProperty(
        prefix = "roost.webhook",
        name = "enabled",
        havingValue = "true"
)
public class WebhookAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(WebhookService.class)
    public WebhookService webhookService(
            WebhookSubscriptionRepository subscriptionRepository,
            WebhookProperties webhookProperties) {
        return new WebhookService(subscriptionRepository, webhookProperties);
    }
}
