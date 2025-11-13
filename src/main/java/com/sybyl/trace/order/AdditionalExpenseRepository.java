package com.sybyl.trace.order;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalExpenseRepository extends JpaRepository<AdditionalExpense, Long> {
	@EntityGraph(attributePaths = {"uploadedBy", "label", "vertical"})
  List<AdditionalExpense> findByOrderIdOrderByUploadedOnDesc(Long orderId);
}

