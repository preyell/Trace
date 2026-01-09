package com.sybyl.trace.order;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.order.events.OrderCreatedEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderCreatedListener {

    private final JavaMailSender mailSender;
    private final AppAuditService auditService;

    public OrderCreatedListener(JavaMailSender mailSender,
                                AppAuditService auditService) {
        this.mailSender = mailSender;
        this.auditService = auditService;
    }

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

        try {
            var msg = new org.springframework.mail.SimpleMailMessage();
            msg.setTo(e.salesManagerEmail());
            msg.setSubject("New Order " + e.salesOrderId());
            msg.setText("""
                    A new order has been created.

                    Sales Order ID: %s
                    Customer      : %s
                    Location      : %s
                    Description   : %s

                    Please log in to Trace to review.
                    """.formatted(
                    e.salesOrderId(),
                    e.customerName(),
                    e.locationLabel(),
                    e.description() == null ? "" : e.description()
            ));

            mailSender.send(msg);

            

            log.info("OrderCreatedListener email sent: soid={} to={}",
                    e.salesOrderId(), e.salesManagerEmail());

        } catch (Exception ex) {
            log.error("OrderCreatedListener email send failed: soid={} to={}",
                    e.salesOrderId(), e.salesManagerEmail(), ex);

            
        }
    }
}
