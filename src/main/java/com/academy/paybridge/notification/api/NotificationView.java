package com.academy.paybridge.notification.api;

import java.time.Instant;
import java.util.UUID;

public record NotificationView(UUID id, UUID customerId, NotificationChannel channel, String destination,
                               String message, boolean delivered, Instant createdAt) {}
