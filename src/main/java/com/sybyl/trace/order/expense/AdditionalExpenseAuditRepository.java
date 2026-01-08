package com.sybyl.trace.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalExpenseAuditRepository extends JpaRepository<AdditionalExpenseAudit, Long> {
  List<AdditionalExpenseAudit> findByExpenseIdOrderByActedOnDesc(Long expenseId);
}

