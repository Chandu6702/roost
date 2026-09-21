package com.chandu.roost.notification;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NotificationChannelService {

    private final Map<Long, NotificationChannel> notificationChannels = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Iterable<NotificationChannel> getAll() {
        return notificationChannels.values();
    }

    public NotificationChannel getById(Long id) {
        NotificationChannel channel = notificationChannels.get(id);

        if (channel == null) {
            throw new NotificationChannelNotFoundException(id);
        }

        return channel;
    }

    public NotificationChannel create(NotificationChannel notificationChannel) {
        Long id = nextId.getAndIncrement();

        notificationChannel.setId(id);
        notificationChannels.put(id, notificationChannel);

        return notificationChannel;
    }

    public NotificationChannel update(
            Long id,
            NotificationChannel notificationChannel) {

        if (!notificationChannels.containsKey(id)) {
            throw new NotificationChannelNotFoundException(id);
            }

        notificationChannel.setId(id);
        notificationChannels.put(id, notificationChannel);

        return notificationChannel;
    }

    public void delete(Long id) {
        if (!notificationChannels.containsKey(id)) {
            throw new NotificationChannelNotFoundException(id);
        }

        notificationChannels.remove(id);
    }
}