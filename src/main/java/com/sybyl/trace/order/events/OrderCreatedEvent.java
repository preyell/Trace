// com.sybyl.trace.order.events.OrderCreatedEvent.java
package com.sybyl.trace.order.events;

public record OrderCreatedEvent(
	    Long orderId,
	    Long customerId,
	    String customerName,
	    String description,
	    String locationLabel,
	    Long salesManagerId,
	    String salesManagerEmail,
	    String salesOrderId // NEW
	) {}
