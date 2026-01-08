package com.sybyl.trace.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    @Value("${app.notifications.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.notifications.cleanup.days:90}")
    private int cleanupDays;

    private final NotificationService notificationService;

    /**
     * Controlled by application.properties using:
     * trace.notifications.cleanup.cron
     */
    @Scheduled(cron = "${app.notifications.cleanup.cron}")
    public void cleanupOldRead() {
        if (!cleanupEnabled) {
            log.info("Notification cleanup is disabled. Skipping run.");
            return;
        }

        int deleted = notificationService.cleanupOldReadNotifications(cleanupDays);
        log.info("Notification cleanup job completed. days={}, deleted={}", cleanupDays, deleted);
    }
}
