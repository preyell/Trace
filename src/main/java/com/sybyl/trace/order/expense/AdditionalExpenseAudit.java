// com.sybyl.trace.order.AdditionalExpenseAudit.java
package com.sybyl.trace.order;

import java.time.Instant;
import com.sybyl.trace.user.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "additional_expense_audit")
@Getter
@Setter
public class AdditionalExpenseAudit {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "expense_id", nullable = false)
	private AdditionalExpense expense;
	@ManyToOne
	private AppUser actor;

	@Column(nullable = false)
	private String action;
	@Column(columnDefinition = "text")
	private String note;
	@Column(nullable = false)
	private Instant actedOn;

	@jakarta.persistence.Transient
	public java.util.Date getActedOnDate() {
		return actedOn == null ? null : java.util.Date.from(actedOn);
	}

}
