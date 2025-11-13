package com.sybyl.trace.order;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sybyl.trace.order.events.OrderCreatedEvent;

@Component
public class OrderCreatedListener {
  private final JavaMailSender mailSender;

  public OrderCreatedListener(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onOrderCreated(OrderCreatedEvent e) {
    if (e.salesManagerEmail() == null || e.salesManagerEmail().isBlank()) return;

    var msg = new org.springframework.mail.SimpleMailMessage();
    msg.setTo(e.salesManagerEmail());
    msg.setSubject("New Order " + e.salesOrderId()); // show SOID, not numeric id
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
        )
    );
    mailSender.send(msg);
  }

}
