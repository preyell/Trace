package com.sybyl.trace.order.expense;

import java.math.BigDecimal;

public class OrderAdditionalExpenseSummaryRow {

    private final Long orderId;
    private final String salesOrderId;
    private final String customerName;
    private final BigDecimal totalExpenseUsd;
    private final BigDecimal totalConsumedUsd;
    private final BigDecimal remainingUsd;

    public OrderAdditionalExpenseSummaryRow(Long orderId,
                                            String salesOrderId,
                                            String customerName,
                                            BigDecimal totalExpenseUsd,
                                            BigDecimal totalConsumedUsd) {
        this.orderId = orderId;
        this.salesOrderId = salesOrderId;
        this.customerName = customerName;
        this.totalExpenseUsd = totalExpenseUsd != null ? totalExpenseUsd : BigDecimal.ZERO;
        this.totalConsumedUsd = totalConsumedUsd != null ? totalConsumedUsd : BigDecimal.ZERO;
        this.remainingUsd = this.totalExpenseUsd.subtract(this.totalConsumedUsd);
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getSalesOrderId() {
        return salesOrderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getTotalExpenseUsd() {
        return totalExpenseUsd;
    }

    public BigDecimal getTotalConsumedUsd() {
        return totalConsumedUsd;
    }

    public BigDecimal getRemainingUsd() {
        return remainingUsd;
    }
}
