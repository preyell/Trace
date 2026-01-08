package com.sybyl.trace.notification;

import java.time.Instant;
import java.util.Date;

import com.sybyl.trace.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who should see this notification
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private AppUser recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 64, nullable = false)
    private NotificationType type;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "text", nullable = false)
    private String message;

    // For linking to detail pages
    @Column(name = "target_type", length = 64)
    private String targetType;     // e.g. "ORDER", "MARGIN", "EXPENSE"

    @Column(name = "target_id")
    private Long targetId;         // id of order / margin / expense

    @Column(name = "target_url", length = 512)
    private String targetUrl;      // e.g. "/orders/11?tab=margin"

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag = false;

    // --- getters/setters ---

    public Long getId() {
        return id;
    }

    public AppUser getRecipient() {
        return recipient;
    }

    public void setRecipient(AppUser recipient) {
        this.recipient = recipient;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isReadFlag() {
        return readFlag;
    }

    public void setReadFlag(boolean readFlag) {
        this.readFlag = readFlag;
    }
    
    @Transient
    public Date getCreatedAtDate() {
        return createdAt == null ? null : Date.from(createdAt);
    }
}
