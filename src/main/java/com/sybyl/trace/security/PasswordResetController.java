package com.sybyl.trace.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.user.AppUserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class PasswordResetController {

  private final AppUserRepository users;
  private final PasswordResetTokenRepository tokens;
  private final JavaMailSender mail;
  private final PasswordEncoder encoder;
  private final String baseUrl;
  private final AppAuditService appAuditService;

  public PasswordResetController(
      AppUserRepository users,
      PasswordResetTokenRepository tokens,
      JavaMailSender mail,
      PasswordEncoder encoder,
      @org.springframework.beans.factory.annotation.Value("${app.public-base-url}") String baseUrl,
      AppAuditService appAuditService
  ) {
    this.users = users;
    this.tokens = tokens;
    this.mail = mail;
    this.encoder = encoder;
    this.baseUrl = baseUrl;
    this.appAuditService = appAuditService;
  }

  @GetMapping("/forgot-password")
  public String forgotForm(Model model) {
    return "auth/forgot";
  }

  @PostMapping("/forgot-password")
  public String sendLink(@RequestParam String email, Model model, HttpServletRequest request) {

    String ip = request.getRemoteAddr();
    String safeEmail = (email == null ? "" : email.trim());

    log.info("Password reset requested: email={}, ip={}", safeEmail, ip);

    AppUser u = users.findByEmailIgnoreCase(safeEmail).orElse(null);

    // Always respond success to avoid user enumeration
    if (u != null && u.isEnabled()) {

      var token = new PasswordResetToken();
      token.setUser(u);
      token.setToken(UUID.randomUUID().toString().replace("-", ""));
      token.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
      tokens.save(token);

      String link = baseUrl + "/reset-password?token=" + token.getToken();

      var msg = new SimpleMailMessage();
      msg.setTo(u.getEmail());
      msg.setSubject("Reset your Trace password");
      msg.setText("""
          Hi %s,

          Use the link below to set a new password. It expires in 2 hours.
          %s

          — Trace
          """.formatted(u.getFirstName(), link));
      mail.send(msg);

      log.info("Password reset token created and email sent: username={}, ip={}", u.getUsername(), ip);

      appAuditService.logEvent(
          "AUTH",
          u.getId(),
          null,
          "PASSWORD_RESET_TOKEN_CREATED",
          "Password reset token created for user " + u.getUsername(),
          null,
          u,
          ip
      );
    } else {
      log.info("Password reset requested for non-existing/disabled email (still returning success): email={}, ip={}",
          safeEmail, ip);
    }

    model.addAttribute("message", "If an account exists for that email, a reset link has been sent.");
    return "auth/forgot";
  }

  @GetMapping("/reset-password")
  public String resetForm(@RequestParam String token, Model model) {
    var t = tokens.findActiveByTokenFetchUser(token, Instant.now()).orElse(null);
    if (t == null) {
      model.addAttribute("error", "Reset link is invalid or expired.");
      log.warn("Reset form opened with invalid/expired token");
    }
    model.addAttribute("token", token);
    return "auth/reset";
  }

  @PostMapping("/reset-password")
  public String doReset(@RequestParam String token,
                        @RequestParam String password,
                        @RequestParam String confirm,
                        Model model,
                        HttpServletRequest request) {

    String ip = request.getRemoteAddr();

    if (password == null || confirm == null || !password.equals(confirm) || password.length() < 8) {
      model.addAttribute("error", "Passwords must match and be at least 8 characters.");
      model.addAttribute("token", token);

      log.warn("Password reset failed validation: ip={}", ip);

      return "auth/reset";
    }

    var now = Instant.now();
    var t = tokens.findActiveByTokenFetchUser(token, now).orElse(null);
    if (t == null) {
      model.addAttribute("error", "Reset link is invalid or expired.");
      model.addAttribute("token", token);

      log.warn("Password reset failed: invalid/expired token, ip={}", ip);

      return "auth/reset";
    }

    var u = t.getUser();
    u.setPassword(encoder.encode(password));
    users.save(u);

    t.setUsedAt(now);
    tokens.save(t);

    log.info("Password reset success: username={}, ip={}", u.getUsername(), ip);

    return "redirect:/login?reset=success";
  }
}
