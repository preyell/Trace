package com.sybyl.trace.audit;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppAuditLogRepository extends JpaRepository<AppAuditLog, Long> {

    List<AppAuditLog> findTop200ByOrderByEventTimeDesc();

    @Query("""
        select a
        from AppAuditLog a
        where
            (:q is null or :q = '' or
                lower(a.message) like lower(concat('%', :q, '%')) or
                lower(a.entityType) like lower(concat('%', :q, '%')) or
                lower(a.actorUsername) like lower(concat('%', :q, '%')) or
                lower(a.actorDisplayName) like lower(concat('%', :q, '%')) or
                cast(a.salesOrderId as string) like concat('%', :q, '%') or
                cast(a.entityId as string) like concat('%', :q, '%')
            )
    """)
    Page<AppAuditLog> search(@Param("q") String q, Pageable pageable);
}
