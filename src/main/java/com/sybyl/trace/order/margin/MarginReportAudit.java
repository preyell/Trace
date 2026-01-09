// com.sybyl.trace.order.MarginReportAudit.java
package com.sybyl.trace.order.margin;

import java.time.Instant;

import com.sybyl.trace.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "margin_report_audit")
public class MarginReportAudit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;



  @Column(name = "actor_id", insertable = false, updatable = false)
  private Long actorId;

  @Column(name = "action", length = 30, nullable = false) // CREATED, UPDATED, APPROVED, DELETED
  private String action;

  @Column(name = "acted_on", nullable = false)
  private Instant actedOn = Instant.now();

  @Column(name = "note", length = 1000)
  private String note;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "margin_report_id", nullable = false,
              foreignKey = @ForeignKey(name = "fk_mra_mr"))
  private MarginReport marginReport;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id")
  private AppUser actor;
}
