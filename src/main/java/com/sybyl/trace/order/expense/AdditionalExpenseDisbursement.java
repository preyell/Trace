// com.sybyl.trace.order.AdditionalExpenseDisbursement.java
package com.sybyl.trace.order.expense;

import java.math.BigDecimal;
import java.time.Instant;

import com.sybyl.trace.order.CurrencyCode;
import com.sybyl.trace.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "additional_expense_disbursement")
@Getter
@Setter
public class AdditionalExpenseDisbursement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "expense_id", nullable = false)
	private AdditionalExpense expense; // NO cascade here

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private AppUser actor;

	@Column(name = "amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency", length = 8, nullable = false)
	private CurrencyCode currency; // same enum you use on expense

	@Column(name = "amount_usd", nullable = false, precision = 18, scale = 2)
	private BigDecimal amountUsd;

	@Column(name = "conversion_rate", precision = 18, scale = 6)
	private BigDecimal conversionRate;

	@Column(name = "disbursed_on", nullable = false)
	private Instant disbursedOn;

	@Column(name = "note", columnDefinition = "text")
	private String note;

	@Transient
	public java.util.Date getDisbursedOnDate() {
		return disbursedOn == null ? null : java.util.Date.from(disbursedOn);
	}

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = Instant.now();
	}

	@Transient
	public java.util.Date getCreatedAtDate() {
		return createdAt == null ? null : java.util.Date.from(createdAt);
	}
}
