package com.sybyl.trace.order.finance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderInvoiceRepository extends JpaRepository<OrderInvoice, Long> {

    List<OrderInvoice> findByOrderId(Long orderId);
}
