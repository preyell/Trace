package com.sybyl.trace.audit;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sybyl.trace.user.AppUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppAuditService {

    private final AppAuditLogRepository repo;

    @Transactional
    public void logEvent(
            String entityType,   // "ORDER", "ADDITIONAL_EXPENSE", "MARGIN_REPORT", ...
            Long entityId,       // e.g. 10L
            String orderId,      // Sales order ID, if applicable (can be null)
            String action,       // "DELETE", "CREATE", "DISBURSE", ...
            String message,      // "Deleted additional expense 5 on order 10"
            String detailsJson,  // optional JSON or null
            AppUser actor,       // can be null
            String actorIp       // can be null
    ) {
        if (log.isDebugEnabled()) {
            log.debug("Creating audit event: entityType={}, entityId={}, orderId={}, action={}, actor={}, ip={}",
                    entityType, entityId, orderId,
                    action,
                    actor != null ? actor.getUsername() : "SYSTEM",
                    actorIp);
        }

        AppAuditLog e = new AppAuditLog();
        e.setEventTime(Instant.now());
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setSalesOrderId(orderId);
        e.setAction(action);
        e.setMessage(message);
        e.setDetailsJson(detailsJson);

        if (actor != null) {
            e.setActorUsername(actor.getUsername());
            e.setActorDisplayName(actor.getFirstName() + " " + actor.getLastName());
        } else {
            e.setActorUsername("SYSTEM");
            e.setActorDisplayName("System");
        }

        e.setActorIp(actorIp);

        repo.save(e);

        // Technical log line to file
        log.info("[AUDIT] {} {} (entityId={}, orderId={}) by {} from {} - {}",
                entityType,
                action,
                entityId,
                orderId,
                e.getActorUsername(),
                actorIp,
                message);
    }

    @Transactional(readOnly = true)
    public List<AppAuditLog> latest() {
        log.debug("Fetching latest 200 audit events");
        return repo.findTop200ByOrderByEventTimeDesc();
    }

    @Transactional(readOnly = true)
    public Page<AppAuditLog> search(String q, int page, int size) {
        int pageIndex = Math.max(page, 0);
        int pageSize  = (size <= 0) ? 20 : size;

        if (log.isDebugEnabled()) {
            log.debug("Searching audit logs: q='{}', page={}, size={}", q, pageIndex, pageSize);
        }

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(Sort.Direction.DESC, "eventTime")
        );

        Page<AppAuditLog> result;
        if (q == null || q.isBlank()) {
            result = repo.findAll(pageable);
        } else {
            result = repo.search(q.trim(), pageable);
        }

        log.debug("Audit log search result: totalElements={}, totalPages={}",
                result.getTotalElements(), result.getTotalPages());

        return result;
    }
}
