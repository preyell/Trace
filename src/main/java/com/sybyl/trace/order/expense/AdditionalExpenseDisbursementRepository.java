package com.sybyl.trace.order.expense;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdditionalExpenseDisbursementRepository extends JpaRepository<AdditionalExpenseDisbursement, Long> {
	List<AdditionalExpenseDisbursement> findByExpenseIdOrderByDisbursedOnDesc(Long expenseId);

	@Query("select coalesce(sum(d.amountUsd), 0) from AdditionalExpenseDisbursement d where d.expense.id = :expenseId")
	BigDecimal sumUsdByExpense(@Param("expenseId") Long expenseId);

	void deleteByIdAndExpenseId(Long id, Long expenseId);

	@Query("""
			select d
			from AdditionalExpenseDisbursement d
			left join fetch d.actor
			where d.expense.id = :expenseId
			order by d.disbursedOn desc
			""")
	List<AdditionalExpenseDisbursement> findByExpenseIdWithActor(Long expenseId);

}
