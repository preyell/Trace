package com.sybyl.trace.order;

public enum OrderStatusView {

    // Margin report–driven statuses
    MR_FINANCE_PENDING("Pending approval from Finance"),
    MR_CEO_PENDING("Pending approval from CEO"),
    MR_REJECTED("Rejected"),
    MR_APPROVED_NO_EXPENSE("Approved"),

    // Additional expense–driven statuses
    EXP_CEO_PENDING("Additional expense: Pending CEO approval"),
    EXP_CFO_PENDING("Additional expense: Pending CFO approval"),
    EXP_CFO_APPROVED("Approved"),
    EXP_REJECTED("Additional expense rejected"),

    // Edge case
    NO_MARGIN("No margin report");

    private final String label;

    OrderStatusView(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
