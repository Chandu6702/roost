package com.chandu.roost.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Webhooks", description = "Register endpoints that receive signed incident events")
@RestController
@RequestMapping("/webhooks")
public class WebhookSubscriptionController {

    private final WebhookSubscriptionService subscriptionService;

    public WebhookSubscriptionController(
            WebhookSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Operation(summary = "Register a webhook subscription for your workspace")
    @PostMapping
    public ResponseEntity<WebhookSubscriptionResponse> register(
            @Valid @RequestBody WebhookSubscriptionRequest request,
            Authentication authentication) {

        WebhookSubscriptionResponse created =
                subscriptionService.register(request, authentication.getName());

        return ResponseEntity
                .created(URI.create("/webhooks/" + created.id()))
                .body(created);
    }

    @Operation(summary = "List active webhook subscriptions for your workspace")
    @GetMapping
    public List<WebhookSubscriptionResponse> list(Authentication authentication) {
        return subscriptionService.list(authentication.getName());
    }

    @Operation(summary = "Deactivate a webhook subscription")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id, Authentication authentication) {

        subscriptionService.deactivate(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
