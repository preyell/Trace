// com.sybyl.trace.order.AdditionalExpenseAudit.java
package com.sybyl.trace.order.expense;

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
	@JoinColumn(name = "expense_id", nullable = true)
	private AdditionalExpense expense;
	@ManyToOne(fetch = FetchType.EAGER)
	private AppUser actor;

	@Column(nullable = false)
	private String action;
	@Column(columnDefinition = "text")
	private String note;
	@Column(nullable = false)
	private Instant actedOn;
	
	@Column(name = "comments", length = 1000)
	private String comments;

	@jakarta.persistence.Transient
	public java.util.Date getActedOnDate() {
		return actedOn == null ? null : java.util.Date.from(actedOn);
	}

}
