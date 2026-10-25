package com.academy.paybridge.notification.domain;

import com.academy.paybridge.account.api.AccountApi;
import com.academy.paybridge.account.api.AccountView;
import com.academy.paybridge.customer.api.CustomerApi;
import com.academy.paybridge.customer.api.CustomerView;
import com.academy.paybridge.notification.api.NotificationChannel;
import com.academy.paybridge.notification.api.NotificationView;
import com.academy.paybridge.notification.client.NotificationSender;
import com.academy.paybridge.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final NotificationSender sender;
    private final AccountApi accounts;
    private final CustomerApi customers;

    public NotificationService(NotificationRepository notifications, NotificationSender sender,
                               AccountApi accounts, CustomerApi customers) {
        this.notifications = notifications;
        this.sender = sender;
        this.accounts = accounts;
        this.customers = customers;
    }

    /** Sends an SMS and an email to whoever owns the account. */
    @Transactional
    public void notifyAccountHolder(UUID accountId, String message) {
        AccountView account = accounts.getById(accountId);
        if (account.customerId() == null) {
            log.debug("Account {} has no customer; skipping notification", account.accountNumber());
            return;
        }
        CustomerView customer = customers.getById(account.customerId());
        deliver(customer.id(), NotificationChannel.SMS, customer.phoneNumber(), message);
        deliver(customer.id(), NotificationChannel.EMAIL, customer.email(), message);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> recentFor(UUID customerId) {
        return notifications.findTop50ByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(Notification::toView).toList();
    }

    /** A failed SMS must never undo a payment, so errors are recorded, not thrown. */
    private void deliver(UUID customerId, NotificationChannel channel, String destination, String message) {
        boolean delivered = true;
        try {
            sender.send(channel, destination, message);
        } catch (RuntimeException e) {
            delivered = false;
            log.warn("Could not send {} to customer {}", channel, customerId, e);
        }
        notifications.save(new Notification(customerId, channel, destination, message, delivered));
    }
}
