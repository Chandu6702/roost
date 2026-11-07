package com.chandu.roost.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookSubscriptionRepository
        extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByWorkspaceIdAndActiveTrue(Long workspaceId);
}
