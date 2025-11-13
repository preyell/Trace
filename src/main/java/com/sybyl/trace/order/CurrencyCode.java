package com.sybyl.trace.order;

public enum CurrencyCode {
	KES("KES"), TZS("TZS"), USD("USD");

	private String label;

	CurrencyCode(String str) {
		this.label = str;
	}

	public String getLabel() {
		return this.label;
	}

}