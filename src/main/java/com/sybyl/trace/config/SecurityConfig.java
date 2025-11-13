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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Configuration
public class SecurityConfig {
	 @Bean
	  SecurityFilterChain securityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http,
	                                          AuthenticationSuccessHandler otpSuccessHandler) throws Exception {
	    // AuthorizationManager that only allows requests if OTP was verified
	    AuthorizationManager<RequestAuthorizationContext> otpGate = (authentication, context) -> {
	      HttpServletRequest req = context.getRequest();
	      HttpSession session = req.getSession(false);
	      boolean verified = session != null && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"));
	      return new AuthorizationDecision(verified);
	    };
		http.authorizeHttpRequests(auth -> auth
				// 👇 allow internal forwards to JSPs and error pages
				.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

				// your public endpoints
				.requestMatchers("/login", "/error", "/otp", "/forgot-password", "/reset-password", "/activate", "/activate/**", "/webjars/**", "/resources/**", "/static/**", "/css/**", "/js/**",
						"/images/**")
				.permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.anyRequest().access(otpGate))
				.formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login") // POST
						// on username/password success -> generate OTP and redirect to /otp
				        .successHandler(otpSuccessHandler)																							// action
						.failureUrl("/login?error=true").permitAll())
				.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true").permitAll())
				.csrf(Customizer.withDefaults());

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
