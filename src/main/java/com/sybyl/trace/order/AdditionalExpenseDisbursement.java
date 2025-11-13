// com.sybyl.trace.order.AdditionalExpenseDisbursement.java
package com.sybyl.trace.order;

import java.math.BigDecimal;
import java.time.Instant;
import com.sybyl.trace.user.AppUser;
import jakarta.persistence.*;
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

	@Column(name = "disbursed_on", nullable = false)
	private Instant disbursedOn;

	@Column(name = "note", columnDefinition = "text")
	private String note;
}
