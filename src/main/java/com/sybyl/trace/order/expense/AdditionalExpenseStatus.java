// com.sybyl.trace.order.AdditionalExpenseStatus.java
package com.sybyl.trace.order.expense;
public enum AdditionalExpenseStatus {
  WAITING,       // awaiting CEO
  CEO_APPROVED,  // awaiting CFO
  CFO_APPROVED,  // fully approved
  REJECTED
}
