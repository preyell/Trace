package com.sybyl.trace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        
        AuthorizationManager<RequestAuthorizationContext> otpGate = (authentication, context) -> {
            HttpServletRequest req = context.getRequest();
            HttpSession session = req.getSession(false);
            
            // Allow access ONLY if the user has been fully programmatically authenticated AND completed OTP verification
            boolean verified = session != null 
                    && authentication.get().isAuthenticated() 
                    && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"));
            
            return new AuthorizationDecision(verified);
        };

        http
            .authorizeHttpRequests(auth -> auth
                // Allow internal forwards to JSPs and system error views
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                .requestMatchers(
                    "/login", 
                    "/login/send-otp", 
                    "/otp", 
                    "/otp/resend", 
                    "/error", 
                    "/webjars/**", 
                    "/resources/**", 
                    "/static/**", 
                    "/css/**", 
                    "/js/**",
                    "/images/**"
                ).permitAll()

                // 3. Admin-restricted endpoints still check roles first
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // 4. Any other application request (like /orders, /approve, etc.) must clear the OTP Gate
                .anyRequest().access(otpGate)
            )
            // When users go to /login, our custom PasswordlessAuthController intercepts it.
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(Customizer.withDefaults());

        return http.build();
    }

    // Keep this defined so that other components/services compiling user entities do not break
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}