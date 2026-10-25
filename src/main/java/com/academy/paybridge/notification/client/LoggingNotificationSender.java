package com.academy.paybridge.notification.client;

import com.academy.paybridge.notification.api.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Prints messages to the console instead of sending them. */
@Component
@ConditionalOnProperty(name = "paybridge.notification.provider", havingValue = "log", matchIfMissing = true)
class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(NotificationChannel channel, String destination, String message) {
        log.info("[{} -> {}] {}", channel, destination, message);
    }
}
