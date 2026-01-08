package com.sybyl.trace.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sybyl.trace.user.ActivationService;
import com.sybyl.trace.user.ActivationTokenRepository;

import jakarta.validation.constraints.NotBlank;

@Controller
public class ActivationController {
  private final ActivationTokenRepository tokens;
  private final ActivationService activationService;

  public ActivationController(ActivationTokenRepository tokens, ActivationService activationService) {
    this.tokens = tokens; this.activationService = activationService;
  }

  @GetMapping("/activate")
  @PreAuthorize("permitAll()")
  public String show(@RequestParam("token") String token, Model model) {
    var t = tokens.findByToken(token);
    if (t.isEmpty() || t.get().getExpiresAt().isBefore(java.time.Instant.now())) {
      model.addAttribute("message", "Activation link is invalid or expired.");
      return "activate/invalid"; // /WEB-INF/jsp/activate/invalid.jsp
    }
    model.addAttribute("token", token);
    return "activate/set-password"; // /WEB-INF/jsp/activate/set-password.jsp
  }

  @PostMapping("/activate")
  @PreAuthorize("permitAll()")
  public String submit(@RequestParam("token") String token,
                       @RequestParam("password") @NotBlank String password,
                       @RequestParam("confirm")  @NotBlank String confirm,
                       Model model) {
    if (!password.equals(confirm)) {
      model.addAttribute("token", token);
      model.addAttribute("error", "Passwords do not match");
      return "activate/set-password";
    }
    try {
      activationService.activate(token, password);
    } catch (IllegalArgumentException ex) {
      model.addAttribute("message", ex.getMessage());
      return "activate/invalid";
    }
    return "redirect:/login?activated=true";
  }
}
