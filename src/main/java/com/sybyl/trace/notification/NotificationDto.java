// NotificationDto.java
package com.sybyl.trace.notification;

import java.time.Instant;

public class NotificationDto {

    public Long id;
    public String title;
    public String message;
    public String targetUrl;
    public boolean readFlag;
    public Instant createdAt;

    public static NotificationDto from(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.id        = n.getId();
        dto.title     = n.getTitle();
        dto.message   = n.getMessage();
        dto.targetUrl = n.getTargetUrl();
        dto.readFlag      = n.isReadFlag();
        dto.createdAt = n.getCreatedAt();
        return dto;
    }
}
