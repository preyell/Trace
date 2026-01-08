package com.sybyl.trace.order;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_timeline_event")
@Getter
@Setter
public class OrderTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Instant eventTime;

    @Column(nullable = false, length = 50)
    private String eventType; // "ORDER_CREATED", "EXPENSE_ADDED", "EXPENSE_DELETED", "DISBURSEMENT_MADE", ...

    @Column(length = 200)
    private String actorName; // "Veeru", "CFO", etc.

    @Column(length = 500)
    private String message;   // "Deleted additional expense 45 (UGX 100,000)"

    @Column(columnDefinition = "text")
    private String detailsJson; // optional extra details
}
