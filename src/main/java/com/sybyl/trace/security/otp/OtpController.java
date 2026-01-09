package com.sybyl.trace.security.otp;

import java.time.Instant;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sybyl.trace.audit.AppAuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class OtpController {


  @GetMapping("/otp")
  public String otpForm(Model model, HttpServletRequest request) {
    HttpSession s = request.getSession(false);
    String username = (s != null) ? (String) s.getAttribute("OTP_USERNAME") : null;

    log.debug("OTP form opened: username={}, ip={}", username, request.getRemoteAddr());

    model.addAttribute("pageTitle", "Two-Step Verification");
    model.addAttribute("username", username);
    return "otp";
  }

  @PostMapping("/otp")
  public String verify(@RequestParam("code") String code, HttpServletRequest request) {

    HttpSession s = request.getSession(false);
    if (s == null) return "redirect:/login?error=true";

    String username = (String) s.getAttribute("OTP_USERNAME");
    String expected = (String) s.getAttribute("OTP_CODE");
    Instant expiresAt = (Instant) s.getAttribute("OTP_EXPIRES_AT");

    boolean notExpired = (expiresAt != null && Instant.now().isBefore(expiresAt));
    boolean ok = notExpired && expected != null && expected.equals(code);

    String ip = request.getRemoteAddr();

    if (ok) {
      s.setAttribute("OTP_VERIFIED", Boolean.TRUE);
      s.removeAttribute("OTP_CODE");
      s.removeAttribute("OTP_EXPIRES_AT");

      log.info("OTP verified success: username={}, ip={}", username, ip);

     
      return "redirect:/orders";
    }

    log.warn("OTP verified failed: username={}, notExpired={}, ip={}", username, notExpired, ip);

    

    return "redirect:/otp?error=true";
  }
}
