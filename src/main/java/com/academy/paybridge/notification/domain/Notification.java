package com.academy.paybridge.notification.domain;

import com.academy.paybridge.notification.api.NotificationChannel;
import com.academy.paybridge.notification.api.NotificationView;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** A log of what we sent, so support can answer "did the customer get an alert?". */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notification_customer", columnList = "customerId, createdAt"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean delivered;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {}

    Notification(UUID customerId, NotificationChannel channel, String destination, String message, boolean delivered) {
        this.customerId = customerId;
        this.channel = channel;
        this.destination = destination;
        this.message = message.length() > 500 ? message.substring(0, 500) : message;
        this.delivered = delivered;
        this.createdAt = Instant.now();
    }

    public NotificationView toView() {
        return new NotificationView(id, customerId, channel, destination, message, delivered, createdAt);
    }
}
