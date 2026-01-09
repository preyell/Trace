package com.sybyl.trace.order.margin;

public enum MarginReportApprovalStatus {
	FINANCE_PENDING("Pending approval from Finance"), CEO_PENDING("Pending approval from CEO"),  APPROVED("Approved"), REJECTED("Rejected");

	private String label;

	MarginReportApprovalStatus(String str) {
		this.label = str;
	}

	public String getLabel() {
		return this.label;
	}

}
