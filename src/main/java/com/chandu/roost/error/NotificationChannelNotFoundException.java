package com.chandu.roost.notification;

public class NotificationChannelNotFoundException extends RuntimeException {

    public NotificationChannelNotFoundException(Long id) {
        super("Notification channel with id " + id + " was not found");
    }
}