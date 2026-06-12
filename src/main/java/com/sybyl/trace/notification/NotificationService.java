package com.sybyl.trace.notification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sybyl.trace.user.AppRole;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.user.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repo;
    private final AppUserRepository userRepo;

    @Transactional
    public void notifyUser(AppUser recipient, NotificationType type, String title, String message, String targetType,
                           Long targetId, String targetUrl) {

        if (recipient == null) {
            log.debug("notifyUser called with null recipient; ignoring. type={}, title={}", type, title);
            return;
        }

        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        n.setTargetUrl(targetUrl);
        n.setCreatedAt(Instant.now());
        n.setReadFlag(false);

        repo.save(n);
        log.debug("Notification saved: user={}, type={}, title={}, targetType={}, targetId={}",
                recipient.getUsername(), type, title, targetType, targetId);
    }

    /**
     * Notify all users with a given role (e.g. ROLE_FINANCE, ROLE_CEO).
     */
    @Transactional
    public void notifyRole(AppRole role, NotificationType type, String title, String message, String targetType,
                           Long targetId, String targetUrl) {

        List<AppUser> users = userRepo.findByRoles(role);
        log.debug("notifyRole: role={}, usersFound={}", role, users.size());
        for (AppUser u : users) {
            notifyUser(u, type, title, message, targetType, targetId, targetUrl);
        }
    }

    /**
     * For dropdown / bell: last 50 only.
     */
    @Transactional(readOnly = true)
    public List<Notification> listForUser(AppUser user) {
        return repo.findTop50ByRecipientOrderByCreatedAtDesc(user);
    }

    /**
     * For full /notifications page – paged.
     */
    @Transactional(readOnly = true)
    public Page<Notification> pageForUser(AppUser user, int page, int size, boolean unreadOnly) {
        int pageIndex = Math.max(page, 0);
        int pageSize  = (size <= 0) ? 20 : size;

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        if (unreadOnly) {
            return repo.findByRecipientAndReadFlagFalseOrderByCreatedAtDesc(user, pageable);
        } else {
            return repo.findByRecipientOrderByCreatedAtDesc(user, pageable);
        }
    }

    @Transactional
    public void markAsRead(Long id, AppUser currentUser) {
        repo.findById(id).ifPresent(n -> {
            if (n.getRecipient().getId().equals(currentUser.getId()) && !n.isReadFlag()) {
                n.setReadFlag(true);
                log.debug("Notification {} marked as read by {}", id, currentUser.getUsername());
            } else {
                log.debug("Notification {} not marked read; owner mismatch or already read.", id);
            }
        });
    }

    @Transactional(readOnly = true)
    public long unreadCount(AppUser user) {
        return repo.countByRecipientAndReadFlagFalse(user);
    }

    @Transactional
    public void markAllAsRead(AppUser user) {
        int changed = repo.markAllAsReadByRecipient(user);
        log.debug("markAllAsRead: user={}, changed={}", user.getUsername(), changed);
    }

    
    @Transactional
    public int cleanupOldReadNotifications(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        int deleted = repo.deleteOldReadBefore(cutoff);
        log.info("Cleanup old notifications: days={}, cutoff={}, deleted={}", days, cutoff, deleted);
        return  deleted;
    }
    
    @Transactional(readOnly = true)
    public Notification getForUser(Long notificationId, AppUser currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated.");
        }
        return repo.findByIdAndRecipient(notificationId, currentUser)
                .orElseThrow(() -> new RuntimeException("Notification not found."));
    }
}
