package com.sybyl.trace.order.expense;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdditionalExpenseRepository extends JpaRepository<AdditionalExpense, Long> {
	

	@Query(
	        value = """
	          select new com.sybyl.trace.order.expense.OrderAdditionalExpenseSummaryRow(
	              o.id,
	              o.salesOrderId,
	              c.name,
	              cast(sum(e.amountUsd) as bigdecimal),
	              cast(coalesce((
	                  select sum(d.amountUsd)
	                  from AdditionalExpenseDisbursement d
	                  where d.expense.order = o 
	                    and d.expense.label.name = 'Design And Implementation Services'
	                    and d.expense.approvalStatus = com.sybyl.trace.order.expense.AdditionalExpenseStatus.CFO_APPROVED
	              ), 0.0) as bigdecimal)
	          )
	          from AdditionalExpense e
	            join e.order o
	            join o.customer c
	            join e.label lbl
	          where (lbl.name) = 'Design And Implementation Services'
	            and e.approvalStatus = com.sybyl.trace.order.expense.AdditionalExpenseStatus.CFO_APPROVED
	            and (cast(:searchOrder as string) is null or o.salesOrderId like concat('%', cast(:searchOrder as string), '%'))
	            and (cast(:searchCustomer as string) is null or lower(c.name) like lower(concat('%', cast(:searchCustomer as string), '%')))
	          group by o.id, o.salesOrderId, c.name
	          order by o.id desc
	          """,
	        countQuery = """
	          select count(distinct o.id)
	          from AdditionalExpense e
	            join e.order o
	            join o.customer c
	            join e.label lbl
	          where (lbl.name) = 'Design And Implementation Services'
	            and e.approvalStatus = com.sybyl.trace.order.expense.AdditionalExpenseStatus.CFO_APPROVED
	            and (cast(:searchOrder as string) is null or o.salesOrderId like concat('%', cast(:searchOrder as string), '%'))
	            and (cast(:searchCustomer as string) is null or lower(c.name) like lower(concat('%', cast(:searchCustomer as string), '%')))
	          """
	    )
	    Page<OrderAdditionalExpenseSummaryRow> findOrderExpenseSummary(
	        @Param("searchOrder") String searchOrder,
	        @Param("searchCustomer") String searchCustomer,
	        Pageable pageable
	    );
	@EntityGraph(attributePaths = { "uploadedBy", "label", "vertical" })
	List<AdditionalExpense> findByOrderIdOrderByUploadedOnDesc(Long orderId);

	List<AdditionalExpense> findByOrderId(Long orderId);
	boolean existsByOrderIdAndVerticalId(Long orderId, Long verticalId);
	
	@Modifying
    @Query("DELETE FROM AdditionalExpense e WHERE e.id = :expId")
    void deleteExpenseByIdQuery(@Param("expId") Long expId);
	
	@Query("select distinct o.salesOrderId from AdditionalExpense e join e.order o order by o.salesOrderId")
    List<String> findDistinctSalesOrderIds();

    @Query("select distinct c.name from AdditionalExpense e join e.order o join o.customer c order by c.name")
    List<String> findDistinctCustomerNames();
}