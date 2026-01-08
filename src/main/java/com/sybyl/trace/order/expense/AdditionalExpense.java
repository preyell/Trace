// com.sybyl.trace.order.AdditionalExpense.java
package com.sybyl.trace.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.sybyl.trace.masterdata.AdditionalExpenseLabel;
import com.sybyl.trace.masterdata.Vertical;
import com.sybyl.trace.user.AppUser;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "additional_expense")
@Getter @Setter
public class AdditionalExpense {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false) private Order order;
  @ManyToOne(optional=false) private AdditionalExpenseLabel label;
  @ManyToOne(optional=false) private Vertical vertical;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "uploaded_by")
  private AppUser uploadedBy;
  @Column(nullable=false) private Instant uploadedOn;

  @Column(nullable=false, precision=18, scale=2) private BigDecimal amount;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private CurrencyCode currency;
  @Column(name="conversion_rate", nullable=false, precision=18, scale=6) private BigDecimal conversionRate;
  @Column(name="amount_usd", nullable=false, precision=18, scale=2) private BigDecimal amountUsd;

  @Column(nullable=false, columnDefinition="text") private String comments;

  private String fileName;
  private String storageKey;

  @Enumerated(EnumType.STRING) @Column(nullable=false)
  private AdditionalExpenseStatus approvalStatus = AdditionalExpenseStatus.WAITING;

  @ManyToOne private AppUser ceoApprovedBy;
  private Instant ceoApprovedOn;

  @ManyToOne private AppUser cfoApprovedBy;
  private Instant cfoApprovedOn;

  @ManyToOne private AppUser rejectedBy;
  private Instant rejectedOn;
  @Column(columnDefinition="text") private String rejectionReason;

  @OneToMany(mappedBy="expense", cascade=CascadeType.ALL, orphanRemoval=true)
  private List<AdditionalExpenseDisbursement> disbursements = new ArrayList<>();
  
  @Transient
  public java.util.Date getUploadedOnDate() {
      return uploadedOn == null ? null : java.util.Date.from(uploadedOn);
  }
}
