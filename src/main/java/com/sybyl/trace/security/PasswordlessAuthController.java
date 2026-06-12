package com.sybyl.trace.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sybyl.trace.notification.EmailService;
import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.user.AppUserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class PasswordlessAuthController {

    private final AppUserRepository userRepository;
    private final OtpService otpService;
    private final UserDetailsService userDetailsService; // Loads your MyUserDetails implementation
    @Autowired
    private EmailService emailService;
    // 1. Render the initial Single-Field Login Page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // login.jsp (Only asks for Username/Email)
    }

 // 2. Handle Step 1: Validate User and Dispatch OTP
    @PostMapping("/login/send-otp")
    public String handleLoginRequest(@RequestParam("usernameOrEmail") String input, 
                                     HttpServletRequest request, 
                                     RedirectAttributes redirectAttributes) {
        
        // Find user by either username or email lookup paths
        var userOpt = userRepository.findByUsernameIgnoreCase(input);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailIgnoreCase(input);
        }

        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            log.warn("Login access denied for input string: {}", input);
            redirectAttributes.addFlashAttribute("error", "Account not found or currently disabled.");
            return "redirect:/login";
        }

        AppUser user = userOpt.get();
        String code = otpService.generateCode();
        Instant expiresAt = Instant.now().plusSeconds(300); // 5 minutes validity

        // Attempt to send the email FIRST
        boolean emailSent = emailService.sendOtpEmail(user.getEmail(), code);
        
        // If email fails, abort the login process and show an error
        if (!emailSent) {
            log.error("Aborting login: SMTP failure for user {}", user.getUsername());
            redirectAttributes.addFlashAttribute("error", "System could not send the OTP email. Please contact Admin.");
            return "redirect:/login";
        }

        // Establish the temporary unverified session variables only if email succeeds
        HttpSession session = request.getSession(true);
        session.setAttribute("OTP_USERNAME", user.getUsername());
        session.setAttribute("OTP_CODE", code);
        session.setAttribute("OTP_EXPIRES_AT", expiresAt);
        
        log.info("OTP successfully generated and emailed for: {}", user.getUsername());
        
        //TO DELTE THIS--------------------------------------------------------->
        log.info("[TEST UTILITY] Regenerated Passwordless OTP for {} is: {}", user.getUsername(), code);
        return "redirect:/otp";
    }

    // 3. Render the 6-Box Segmented Verification Page
    @GetMapping("/otp")
    public String showOtpPage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("OTP_USERNAME") == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", session.getAttribute("OTP_USERNAME"));
        return "otp";
    }

    // 4. Handle Step 2: Validate OTP and Programmatically Authenticate
    @PostMapping("/otp")
    public String verifyOtp(@RequestParam("code") String code, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("OTP_USERNAME") == null) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("OTP_USERNAME");
        String expectedCode = (String) session.getAttribute("OTP_CODE");
        Instant expiresAt = (Instant) session.getAttribute("OTP_EXPIRES_AT");

        boolean isValid = expiresAt != null && Instant.now().isBefore(expiresAt) && expectedCode.equals(code);

        if (isValid) {
            // SUCCESS: Load core UserDetails profile configurations
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Programmatically establish Spring Security's authenticated token
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            
            // Inject into the primary container execution context thread
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // ✅ CRITICAL FIX: Set the OTP verified state to TRUE so the otpGate opens!
            session.setAttribute("OTP_VERIFIED", Boolean.TRUE);
            
            // Save the authenticated state explicitly into the underlying HTTP Session
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Clear temporary OTP inputs out of session memory bounds
            session.removeAttribute("OTP_CODE");
            session.removeAttribute("OTP_EXPIRES_AT");

            log.info("User '{}' successfully authenticated via Passwordless OTP gateway.", username);
            return "redirect:/orders";
        }

        log.warn("Invalid or expired OTP submission tracking for user context: {}", username);
        return "redirect:/otp?error=true";
    }

    // 5. Handle Throttled Resend Action Mapping
 // 5. Handle Throttled Resend Action Mapping
    @PostMapping("/otp/resend")
    public String handleResendRequest(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("OTP_USERNAME") == null) {
            return "redirect:/login";
        }

        String username = (String) session.getAttribute("OTP_USERNAME");
        String freshCode = otpService.generateCode();
        Instant freshExpiry = Instant.now().plusSeconds(300);

        // Fetch user from DB to get the email address
        var userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            
            // Trigger the email service
            boolean emailSent = emailService.sendOtpEmail(user.getEmail(), freshCode);
            
            if (!emailSent) {
                redirectAttributes.addFlashAttribute("error", "Failed to resend OTP email. Please try again later.");
                return "redirect:/otp";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Session invalid. User not found.");
            return "redirect:/login";
        }

        // Update the session only if the email was successfully dispatched
        session.setAttribute("OTP_CODE", freshCode);
        session.setAttribute("OTP_EXPIRES_AT", freshExpiry);
      //TO DELTE THIS--------------------------------------------------------->
        log.info("[TEST UTILITY] Regenerated Passwordless OTP for {} is: {}", username, freshCode);
        log.info("Regenerated Passwordless OTP successfully emailed for: {}", username);
        redirectAttributes.addFlashAttribute("resendSuccess", true);
        return "redirect:/otp";
    }
}