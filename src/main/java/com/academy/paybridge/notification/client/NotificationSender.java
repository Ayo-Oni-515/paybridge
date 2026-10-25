package com.academy.paybridge.notification.client;

import com.academy.paybridge.notification.api.NotificationChannel;

/** An SMS gateway or email service. Swap the fake for a real provider without touching the listener. */
public interface NotificationSender {

    /** @throws RuntimeException if the provider couldn't accept the message */
    void send(NotificationChannel channel, String destination, String message);
}
