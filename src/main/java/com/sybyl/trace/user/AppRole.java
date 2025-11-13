package com.sybyl.trace.user;

public enum AppRole {
    ADMIN, SALES_MANAGER, FINANCE, FINANCE_APPROVER, CEO, CFO;

    /** Converts to Spring Security authority name */
    public String asAuthority() {
        return "ROLE_" + name();
    }

    /** Nice label for dropdowns */
    public String label() {
        return switch (this) {
            case ADMIN -> "Admin";
            case SALES_MANAGER -> "Sales Manager";
            case FINANCE -> "Finance";
            case FINANCE_APPROVER -> "Finance Approver";
            case CEO -> "CEO";
            case CFO -> "CFO";
        };
    }
}
