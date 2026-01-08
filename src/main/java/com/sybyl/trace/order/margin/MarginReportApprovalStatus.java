package com.sybyl.trace.order;

public enum ApprovalStatus {
	FINANCE_PENDING("Pending approval from Finance"), CEO_PENDING("Pending approval from CEO"),  APPROVED("Approved"), REJECTED("Rejected");

	private String label;

	ApprovalStatus(String str) {
		this.label = str;
	}

	public String getLabel() {
		return this.label;
	}

}
