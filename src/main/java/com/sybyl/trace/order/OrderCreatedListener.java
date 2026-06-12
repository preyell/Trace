package com.sybyl.trace.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.notification.EmailService;
import com.sybyl.trace.order.events.OrderCreatedEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderCreatedListener {


    @Autowired
    private EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent e) {

        // TECHNICAL LOGGING
        log.info("OrderCreatedListener triggered: salesOrderId={}, customer={} location={}",
                e.salesOrderId(), e.customerName(), e.locationLabel());

        if (e.salesManagerEmail() == null || e.salesManagerEmail().isBlank()) {
            log.warn("OrderCreatedListener skipped: no sales manager email for SOID={}",
                    e.salesOrderId());
            return;
        }

        // Delegate the email formatting and sending to the dedicated service
        boolean emailSent = emailService.sendOrderCreatedEmail(
                e.salesManagerEmail(),
                e.salesOrderId(),
                e.customerName(),
                e.locationLabel(),
                e.description()
        );

        if (!emailSent) {
            log.error("CRITICAL: Failed to notify Sales Manager ({}) for new Order: {}", 
                    e.salesManagerEmail(), e.salesOrderId());
        }
    }
}
