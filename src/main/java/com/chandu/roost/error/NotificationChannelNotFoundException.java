package com.chandu.roost.error;

public class NotificationChannelNotFoundException extends RuntimeException {

    public NotificationChannelNotFoundException(Long id) {
        super("Notification channel with id " + id + " was not found");
    }
}