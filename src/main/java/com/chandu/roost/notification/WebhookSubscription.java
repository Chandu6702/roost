package com.chandu.roost.notification;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A tenant-owned endpoint that should receive signed incident events.
 * The {@code secret} is the HMAC key shared with that subscriber so they can
 * verify each delivery actually came from Roost.
 */
@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 255)
    private String secret;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WebhookSubscription() {
    }

    public WebhookSubscription(Long workspaceId, String url, String secret) {
        this.workspaceId = workspaceId;
        this.url = url;
        this.secret = secret;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getUrl() {
        return url;
    }

    public String getSecret() {
        return secret;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void deactivate() {
        this.active = false;
    }
}
