package com.sybyl.trace.security;

import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.notification.NotificationService;
import com.sybyl.trace.notification.NotificationType;
import com.sybyl.trace.user.ActivationService;
import com.sybyl.trace.user.ActivationTokenRepository;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.web.IpUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ActivationController {

    private final ActivationTokenRepository tokens;
    private final ActivationService activationService;
    private final AppAuditService appAuditService;
    private final NotificationService notificationService;

    @GetMapping("/activate")
    @PreAuthorize("permitAll()")
    public String show(@RequestParam("token") String token,
                       Model model,
                       HttpServletRequest request) {

        String ip = IpUtils.getClientIp(request);
        log.info("Activation page requested for token={} from {}", token, ip);

        var optToken = tokens.findByToken(token);
        if (optToken.isEmpty()) {
            log.warn("Activation token not found: token={} from {}", token, ip);
            model.addAttribute("message", "Activation link is invalid or expired.");
            return "activate/invalid";
        }

        var t = optToken.get();
        Instant now = Instant.now();
        if (t.getExpiresAt() != null && t.getExpiresAt().isBefore(now)) {
            AppUser u = t.getUser();
            Long userId = (u != null ? u.getId() : null);
            String username = (u != null ? u.getUsername() : "unknown");

            log.warn("Activation token expired for userId={}, username={}, token={} from {}",
                    userId, username, token, ip);

            model.addAttribute("message", "Activation link is invalid or expired.");
            return "activate/invalid";
        }

        model.addAttribute("token", token);
        return "activate/set-password";
    }

    @PostMapping("/activate")
    @PreAuthorize("permitAll()")
    public String submit(@RequestParam("token") String token,
                         @RequestParam("password") @NotBlank String password,
                         @RequestParam("confirm")  @NotBlank String confirm,
                         Model model,
                         HttpServletRequest request) {

        String ip = IpUtils.getClientIp(request);
        log.info("Activation submit received for token={} from {}", token, ip);

        if (!password.equals(confirm)) {
            log.warn("Activation password mismatch for token={} from {}", token, ip);
            model.addAttribute("token", token);
            model.addAttribute("error", "Passwords do not match");
            return "activate/set-password";
        }

        var optToken = tokens.findByToken(token);
        if (optToken.isEmpty()) {
            log.warn("Activation submit with invalid token={} from {}", token, ip);
            model.addAttribute("message", "Activation link is invalid or expired.");
            return "activate/invalid";
        }

        var t = optToken.get();
        AppUser user = t.getUser();
        Long userId = (user != null ? user.getId() : null);
        String username = (user != null ? user.getUsername() : "unknown");

        Instant now = Instant.now();
        if (t.getExpiresAt() != null && t.getExpiresAt().isBefore(now)) {
            log.warn("Activation submit with expired token for userId={}, username={}, token={} from {}",
                    userId, username, token, ip);
            return "activate/invalid";
        }

        try {
            activationService.activate(token, password);

            log.info("User activation succeeded for userId={}, username={} from {}",
                    userId, username, ip);

            appAuditService.logEvent(
                    "USER",
                    userId,
                    null,
                    "ACTIVATE_SUCCESS",
                    "User activated account via email link",
                    "{\"username\":\"" + username + "\"}",
                    user,
                    ip
            );

            // Notify admins that this user activated their account
            notificationService.notifyRole(
                    com.sybyl.trace.user.AppRole.ADMIN,
                    NotificationType.USER_ACTIVATED,
                    "User activated: " + username,
                    "User " + username + " has activated their Trace account.",
                    "USER",
                    userId,
                    "/admin/users/" + userId + "/edit"
            );

            return "redirect:/login?activated=true";
        } catch (IllegalArgumentException ex) {
            log.warn("User activation failed for userId={}, username={}, token={} from {} - {}",
                    userId, username, token, ip, ex.getMessage());

            appAuditService.logEvent(
                    "USER",
                    userId,
                    null,
                    "ACTIVATE_FAILED",
                    "User activation failed: " + ex.getMessage(),
                    "{\"username\":\"" + username + "\",\"token\":\"" + token + "\"}",
                    user,
                    ip
            );

            model.addAttribute("message", ex.getMessage());
            return "activate/invalid";
        }
    }
}
