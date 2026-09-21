package com.chandu.roost.controller;

import com.chandu.roost.entity.NotificationChannel;
import com.chandu.roost.service.NotificationChannelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification-channels")
public class NotificationChannelController {

    private final NotificationChannelService notificationChannelService;

    public NotificationChannelController(NotificationChannelService notificationChannelService) {
        this.notificationChannelService = notificationChannelService;
    }

    @GetMapping
    public ResponseEntity<Iterable<NotificationChannel>> getAllNotificationChannels() {
        return ResponseEntity.ok(notificationChannelService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationChannel> getNotificationChannelById(
            @PathVariable Long id) {

        NotificationChannel channel = notificationChannelService.getById(id);
        return ResponseEntity.ok(channel);
    }

    @PostMapping
    public ResponseEntity<NotificationChannel> createNotificationChannel(
            @RequestBody NotificationChannel notificationChannel) {

        NotificationChannel createdChannel =
                notificationChannelService.create(notificationChannel);

        return ResponseEntity.status(201).body(createdChannel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationChannel> updateNotificationChannel(
            @PathVariable Long id,
            @RequestBody NotificationChannel notificationChannel) {

        NotificationChannel updatedChannel =
                notificationChannelService.update(id, notificationChannel);

        return ResponseEntity.ok(updatedChannel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotificationChannel(
            @PathVariable Long id) {

        notificationChannelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}