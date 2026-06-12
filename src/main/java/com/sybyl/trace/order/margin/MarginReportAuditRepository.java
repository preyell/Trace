package com.sybyl.trace.order.margin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
// com.sybyl.trace.order.MarginReportAuditRepository.java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MarginReportAuditRepository extends JpaRepository<MarginReportAudit, Long> {
	@EntityGraph(attributePaths = { "actor", "marginReport", "marginReport.vertical" })
    @Query("select mra from MarginReportAudit mra where mra.marginReport.id = :marginReportId")
    Page<MarginReportAudit> findByMarginReportIdOrderByActedOnDesc(Long marginReportId, Pageable pageable);
}
