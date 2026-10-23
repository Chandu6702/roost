package com.chandu.roost.webhook;

import com.chandu.roost.service.WebhookService;
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
            WebhookProperties webhookProperties) {
        return new WebhookService(webhookProperties);
    }
}
