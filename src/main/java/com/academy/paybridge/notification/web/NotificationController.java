package com.academy.paybridge.notification.web;

import com.academy.paybridge.notification.api.NotificationView;
import com.academy.paybridge.notification.domain.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    /** GET /api/notifications?customerId=... : the 50 most recent alerts. */
    @GetMapping
    public List<NotificationView> recent(@RequestParam UUID customerId) {
        return notifications.recentFor(customerId);
    }
}
