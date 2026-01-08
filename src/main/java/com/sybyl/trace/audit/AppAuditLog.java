package com.sybyl.trace.audit;

import java.time.Instant;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "app_audit_log")
@Getter
@Setter
@ToString(exclude = "detailsJson")
public class AppAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "actor_display_name", length = 150)
    private String actorDisplayName;

    @Column(name = "actor_ip", length = 64)
    private String actorIp;

    @Column(name = "entity_type", length = 100, nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "sales_order_id")
    private String salesOrderId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "details_json", columnDefinition = "text")
    private String detailsJson;

    @Transient
    public Date getEventTimeDate() {
        return eventTime == null ? null : Date.from(eventTime);
    }
}
