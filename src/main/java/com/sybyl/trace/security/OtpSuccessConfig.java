package com.sybyl.trace.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.sybyl.trace.security.otp.OtpService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OtpSuccessConfig {

  private final OtpService otpService;

  @Bean
  public AuthenticationSuccessHandler otpSuccessHandler() {
    return new AuthenticationSuccessHandler() {
      @Override
      public void onAuthenticationSuccess(HttpServletRequest request,
                                          HttpServletResponse response,
                                          Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession(true);

        // Generate & store OTP (and expiry) in session
        String username = authentication.getName();
        String code = otpService.generateCode();
        Instant expiresAt = Instant.now().plusSeconds(300); // 5 minutes

        session.setAttribute("OTP_CODE", code);
        session.setAttribute("OTP_EXPIRES_AT", expiresAt);
        session.setAttribute("OTP_USERNAME", username);
        session.setAttribute("OTP_VERIFIED", Boolean.FALSE);

        // For now, log the OTP to server console (replace with email/SMS later)
        System.out.println("OTP for user " + username + ": " + code + " (valid 5 min)");

        // Redirect to OTP entry page
        response.sendRedirect(request.getContextPath() + "/otp");
      }
    };
  }
}
