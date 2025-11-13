package com.sybyl.trace.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
// com.sybyl.trace.order.MarginReportAuditRepository.java
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarginReportAuditRepository extends JpaRepository<MarginReportAudit, Long> {
	@EntityGraph(attributePaths = { "actor" })
	Page<MarginReportAudit> findByMarginReportIdOrderByActedOnDesc(Long marginReportId, Pageable pageable);
}
