package com.sybyl.trace.order.margin;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarginReportRepository extends JpaRepository<MarginReport, Long> {

    /**
     * Used for listing margin reports on order page
     */
    @EntityGraph(attributePaths = { "vertical", "uploadedBy" })
    @Query("""
        select mr
        from MarginReport mr
        where mr.order.id = :orderId
        order by mr.uploadedOn desc
    """)
    List<MarginReport> findByOrderIdOrderByUploadedOnDesc(@Param("orderId") Long orderId);

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderIdAndVerticalId(Long orderId, Long verticalId);

    boolean existsByOrderIdAndVerticalIdAndIdNot(Long orderId, Long verticalId, Long id);

    List<MarginReport> findByOrderId(Long orderId);

    /**
     * 🔑 CRITICAL FIX
     * Fetch MarginReport together with Order to avoid LazyInitializationException
     * when accessing mr.getOrder().getSalesOrderId() after approval.
     */
    @Query("""
        select mr
        from MarginReport mr
        join fetch mr.order o
        where mr.id = :id
    """)
    Optional<MarginReport> findByIdWithOrder(@Param("id") Long id);
    
    @Query("SELECT mr FROM MarginReport mr LEFT JOIN FETCH mr.vertical WHERE mr.id = :id")
    Optional<MarginReport> findByIdWithVertical(@Param("id") Long id);
}
