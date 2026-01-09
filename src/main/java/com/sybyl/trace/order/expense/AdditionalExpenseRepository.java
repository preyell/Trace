package com.sybyl.trace.order.expense;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdditionalExpenseRepository extends JpaRepository<AdditionalExpense, Long> {

	@Query("""
			select new com.sybyl.trace.order.expense.OrderAdditionalExpenseSummaryRow(
			    o.id,
			    o.salesOrderId,
			    c.name,
			    sum(e.amountUsd),
			    coalesce((
			        select sum(d.amountUsd)
			        from AdditionalExpenseDisbursement d
			        where d.expense.order = o
			    ), 0)
			)
			from AdditionalExpense e
			  join e.order o
			  join o.customer c
			group by o.id, o.salesOrderId, c.name
			order by o.id desc
			""")
	List<OrderAdditionalExpenseSummaryRow> findOrderExpenseSummary();

	@EntityGraph(attributePaths = { "uploadedBy", "label", "vertical" })
	List<AdditionalExpense> findByOrderIdOrderByUploadedOnDesc(Long orderId);

	List<AdditionalExpense> findByOrderId(Long orderId);
	boolean existsByOrderIdAndVerticalId(Long orderId, Long verticalId);

}
