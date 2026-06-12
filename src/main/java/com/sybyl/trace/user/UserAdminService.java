package com.sybyl.trace.user;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.exception.EmailAlreadyInUseException;
import com.sybyl.trace.exception.UsernameAlreadyInUseException;

import com.sybyl.trace.location.Location;
import com.sybyl.trace.masterdata.VerticalRepository;
import com.sybyl.trace.notification.NotificationRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserAdminService {

    private final AppUserRepository users;
    private final VerticalRepository verticals;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final NotificationRepository notificationRepo;
    private final AppAuditService appAuditService;

    public UserAdminService(AppUserRepository users,
                            VerticalRepository verticals,
                            PasswordEncoder encoder,
                            ApplicationEventPublisher events,
                            NotificationRepository notificationRepo,
                            AppAuditService appAuditService) {
        this.users = users;
        this.verticals = verticals;
        this.encoder = encoder;
        this.events = events;
        this.notificationRepo = notificationRepo;
        this.appAuditService = appAuditService;
    }

    @Transactional
    public void createUser(CreateUserRequest req) {
        if (users.existsByUsernameIgnoreCase(req.username()))
            throw new UsernameAlreadyInUseException();
        if (users.existsByEmailIgnoreCase(req.email()))
            throw new EmailAlreadyInUseException();

        AppUser u = new AppUser();
        u.setUsername(req.username().trim());
        u.setEmail(req.email().trim());
        u.setFirstName(req.firstName().trim());
        u.setLastName(req.lastName().trim());
        u.setEnabled(true);
      //  u.setPassword(encoder.encode(Tokens.urlSafe(24))); // placeholder hash

        Set<Location> locations = (req.locations() == null || req.locations().isEmpty())
                ? EnumSet.noneOf(Location.class)
                : EnumSet.copyOf(req.locations());
        u.setLocations(locations);

        Set<AppRole> roles = EnumSet.copyOf(req.roles());
        u.setRoles(roles);

        if (req.verticalIds() != null && !req.verticalIds().isEmpty()) {
            var vs = new HashSet<>(verticals.findAllById(req.verticalIds()));
            if (vs.size() != req.verticalIds().size())
                throw new IllegalArgumentException("One or more vertical IDs are invalid");
            u.setVerticals(vs);
        }

        users.save(u);

        events.publishEvent(new UserCreatedEvent(u.getId(), u.getEmail(), u.getUsername(), u.getFirstName()));

        log.info("User created by admin: userId={}, username={}, email={}", u.getId(), u.getUsername(), u.getEmail());

        appAuditService.logEvent(
            "USER",
            u.getId(),
            null,
            "CREATE",
            "Created user " + u.getUsername() + " (" + u.getEmail() + ")",
            null,
            null
            
        );
    }

    @Transactional
    public void updateUser(Long id, UserForm form) {
        AppUser u = users.findById(id).orElseThrow();

        u.setEmail(form.getEmail());
        u.setFirstName(form.getFirstName());
        u.setLastName(form.getLastName());
        u.setEnabled(form.isEnabled());

        Set<Location> newLocs = (form.getLocations() == null || form.getLocations().isEmpty())
                ? EnumSet.noneOf(Location.class)
                : EnumSet.copyOf(form.getLocations());
        u.setLocations(newLocs);

        Set<AppRole> newRoles = (form.getRoles() == null || form.getRoles().isEmpty())
                ? EnumSet.noneOf(AppRole.class)
                : EnumSet.copyOf(form.getRoles());
        u.setRoles(newRoles);

        Set<Long> ids = (form.getVerticalIds() == null) ? java.util.Set.of() : form.getVerticalIds();
        var vs = new java.util.HashSet<>(verticals.findAllById(ids));
        u.setVerticals(vs);

        users.save(u);

        log.info("User updated by admin: userId={}, username={}", u.getId(), u.getUsername());

        appAuditService.logEvent(
            "USER",
            u.getId(),
            null,
            "UPDATE",
            "Updated user " + u.getUsername(),
            null,
            null
            
        );
    }
    

    @Transactional(readOnly = false)
    public void deleteUser(Long userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        log.warn("Evaluating system-wide dependencies for manual deletion directive: userId={}, username={}", user.getId(), user.getUsername());
        
        // System Administrator safety block
        boolean isSystemAdmin = "admin".equalsIgnoreCase(user.getUsername())
                && user.hasRole(AppRole.ADMIN)
                && "System Administrator".equalsIgnoreCase((user.getFirstName() + " " + user.getLastName()).trim());

        if (isSystemAdmin) {
            throw new IllegalStateException("System Administrator user cannot be deleted.");
        }

        // 1. Proactively check every table for transaction references
        long globalReferenceCount = users.countAllSystemReferences(userId);

        if (globalReferenceCount > 0) {
            log.warn("Deletion aborted: User ID {} has {} active transaction records.", userId, globalReferenceCount);
            
            // Throw an exception to cleanly halt processing and notify the operator
            throw new IllegalStateException("This user cannot be deleted because they are referenced in historic transaction data (Orders, Approvals, or Expenses). You can, howerver, disable the user using 'Edit' option.");
        }

        // 2. Safe Execution Path: Completely clean orphan account
        notificationRepo.deleteByRecipient(user);
        users.delete(user);

        appAuditService.logEvent(
            "USER", userId, null, "DELETE",
            "Permanently deleted user " + user.getUsername(),
            null, null
        );
    }



    public Page<AppUser> search(String q, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by("username").ascending());
        return (q == null || q.isBlank()) ? users.findAll(pageable) : users.search(q.trim(), pageable);
    }
}
