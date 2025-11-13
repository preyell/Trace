// com.sybyl.trace.auth.PasswordResetController
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

import com.sybyl.trace.user.AppUser;
import com.sybyl.trace.user.AppUserRepository;

@Controller
public class PasswordResetController {

	private final AppUserRepository users;
	private final PasswordResetTokenRepository tokens;
	private final JavaMailSender mail;
	private final PasswordEncoder encoder;
	private final String baseUrl;

	public PasswordResetController(AppUserRepository users, PasswordResetTokenRepository tokens, JavaMailSender mail,
			PasswordEncoder encoder,
			@org.springframework.beans.factory.annotation.Value("${app.public-base-url}") String baseUrl) {
		this.users = users;
		this.tokens = tokens;
		this.mail = mail;
		this.encoder = encoder;
		this.baseUrl = baseUrl;
	}

	@GetMapping("/forgot-password")
	public String forgotForm(Model model) {
		return "auth/forgot"; // or reuse your login layout; adjust accordingly
	}

	// handle forgot
	@PostMapping("/forgot-password")
	public String sendLink(@RequestParam String email, Model model) {
		AppUser u = users.findByEmailIgnoreCase(email).orElse(null);

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
		}

		model.addAttribute("message", "If an account exists for that email, a reset link has been sent.");
		return "auth/forgot";
	}

	// show reset form
	@GetMapping("/reset-password")
	public String resetForm(@RequestParam String token, Model model) {
		var t = tokens.findActiveByTokenFetchUser(token, java.time.Instant.now()).orElse(null);
		if (t == null) {
			model.addAttribute("error", "Reset link is invalid or expired.");
		}
		model.addAttribute("token", token); // keep token in the form
		return "auth/reset"; // or your layout approach
	}

	// POST /reset-password
	@PostMapping("/reset-password")
	public String doReset(@RequestParam String token, @RequestParam String password, @RequestParam String confirm,
			Model model) {
		if (!password.equals(confirm) || password.length() < 8) {
			model.addAttribute("error", "Passwords must match and be at least 8 characters.");
			model.addAttribute("token", token);
			return "auth/reset";
		}

		var now = java.time.Instant.now();
		var t = tokens.findActiveByTokenFetchUser(token, now).orElse(null);
		if (t == null) {
			model.addAttribute("error", "Reset link is invalid or expired.");
			model.addAttribute("token", token);
			return "auth/reset";
		}

		var u = t.getUser(); // safe now (user is fetched)
		u.setPassword(encoder.encode(password));
		users.save(u);

		t.setUsedAt(now);
		tokens.save(t);

		return "redirect:/login?reset=success";
	}
}
