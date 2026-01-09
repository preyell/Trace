package com.sybyl.trace.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.sybyl.trace.audit.AppAuditService;
import com.sybyl.trace.security.otp.OtpService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OtpSuccessConfig {

  private final OtpService otpService;
  private final AppAuditService appAuditService;

  @Bean
  public AuthenticationSuccessHandler otpSuccessHandler() {
    return new AuthenticationSuccessHandler() {
      @Override
      public void onAuthenticationSuccess(HttpServletRequest request,
                                          HttpServletResponse response,
                                          Authentication authentication) throws IOException, ServletException {

        HttpSession session = request.getSession(true);

        String username = authentication.getName();
        String code = otpService.generateCode();
        Instant expiresAt = Instant.now().plusSeconds(300); // 5 minutes

        session.setAttribute("OTP_CODE", code);
        session.setAttribute("OTP_EXPIRES_AT", expiresAt);
        session.setAttribute("OTP_USERNAME", username);
        session.setAttribute("OTP_VERIFIED", Boolean.FALSE);

        String ip = request.getRemoteAddr();

        System.out.println("OTP..............." + code);
        // Technical logs (OTP content should NOT be printed)
        log.info("OTP generated for username={}, expiresAt={}, ip={}", username, expiresAt, ip);
        if (log.isDebugEnabled()) {
          log.debug("OTP generated (masked): username={}, otp=****{}", username,
              code != null && code.length() >= 2 ? code.substring(code.length() - 2) : "");
        }

        response.sendRedirect(request.getContextPath() + "/otp");
      }
    };
  }
}
