// com.sybyl.trace.order.MarginReportRepository.java
package com.sybyl.trace.order;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarginReportRepository extends JpaRepository<MarginReport, Long> {

	  @EntityGraph(attributePaths = {"vertical", "uploadedBy"})
	  @Query("select mr from MarginReport mr where mr.order.id = :orderId order by mr.uploadedOn desc")
	  List<MarginReport> findByOrderIdOrderByUploadedOnDesc(@Param("orderId") Long orderId);
	}