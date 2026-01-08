package com.sybyl.trace.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Controller
public class OtpController {

  @GetMapping("/otp")
  public String otpForm(Model model, HttpServletRequest request) {
    // Optional: show the username on the page
    HttpSession s = request.getSession(false);
    String username = (s != null) ? (String) s.getAttribute("OTP_USERNAME") : null;
    model.addAttribute("pageTitle", "Two-Step Verification");
    model.addAttribute("username", username);
    return "otp"; // /WEB-INF/jsp/otp.jsp
  }

  @PostMapping("/otp")
  public String verify(@RequestParam("code") String code, HttpServletRequest request) {
    HttpSession s = request.getSession(false);
    if (s == null) return "redirect:/login?error=true";

    String expected = (String) s.getAttribute("OTP_CODE");
    Instant expiresAt = (Instant) s.getAttribute("OTP_EXPIRES_AT");

    boolean notExpired = (expiresAt != null && Instant.now().isBefore(expiresAt));
    if (notExpired && expected != null && expected.equals(code)) {
      // Mark OTP verified and clean up
      s.setAttribute("OTP_VERIFIED", Boolean.TRUE);
      s.removeAttribute("OTP_CODE");
      s.removeAttribute("OTP_EXPIRES_AT");
      // go to the app
      return "redirect:/orders";
    }
    return "redirect:/otp?error=true";
  }
}
