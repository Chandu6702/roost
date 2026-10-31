package com.chandu.roost.repository;

import com.chandu.roost.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookSubscriptionRepository
        extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByWorkspaceIdAndActiveTrue(Long workspaceId);
}
