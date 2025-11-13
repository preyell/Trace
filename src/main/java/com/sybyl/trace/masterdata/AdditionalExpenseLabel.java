package com.sybyl.trace.masterdata;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter; import lombok.Setter;

@Entity
@Table(name = "additional_expense_label",
       uniqueConstraints = @UniqueConstraint(name = "uq_ael_name", columnNames = "name"))
@Getter @Setter
public class AdditionalExpenseLabel {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 512)
  private String description;

  @Column(name = "is_system", nullable = false)
  private boolean system;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at")
  private Instant updatedAt;
}
