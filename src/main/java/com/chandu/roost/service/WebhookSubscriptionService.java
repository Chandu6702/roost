package com.chandu.roost.service;

import com.chandu.roost.dto.WebhookSubscriptionRequest;
import com.chandu.roost.dto.WebhookSubscriptionResponse;
import com.chandu.roost.entity.WebhookSubscription;
import com.chandu.roost.repository.WebhookSubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Manages a tenant's webhook subscriptions (register / list / deactivate).
 * Separate from {@link WebhookService} (which only delivers) so management is
 * always available even when delivery is disabled, and so the two concerns are
 * independently testable. Every operation is scoped to the caller's workspace.
 */
@Service
public class WebhookSubscriptionService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final CurrentWorkspaceService currentWorkspaceService;

    public WebhookSubscriptionService(
            WebhookSubscriptionRepository subscriptionRepository,
            CurrentWorkspaceService currentWorkspaceService) {
        this.subscriptionRepository = subscriptionRepository;
        this.currentWorkspaceService = currentWorkspaceService;
    }

    @Transactional
    public WebhookSubscriptionResponse register(
            WebhookSubscriptionRequest request, String email) {

        Long workspaceId = currentWorkspaceService.getCurrentWorkspaceId(email);

        WebhookSubscription saved = subscriptionRepository.save(
                new WebhookSubscription(
                        workspaceId, request.url(), request.secret()));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscriptionResponse> list(String email) {
        Long workspaceId = currentWorkspaceService.getCurrentWorkspaceId(email);
        return subscriptionRepository.findByWorkspaceIdAndActiveTrue(workspaceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deactivate(Long id, String email) {
        Long workspaceId = currentWorkspaceService.getCurrentWorkspaceId(email);

        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .filter(s -> s.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Webhook subscription not found"));

        subscription.deactivate();
    }

    private WebhookSubscriptionResponse toResponse(WebhookSubscription s) {
        return new WebhookSubscriptionResponse(
                s.getId(), s.getUrl(), s.isActive(), s.getCreatedAt());
    }
}
