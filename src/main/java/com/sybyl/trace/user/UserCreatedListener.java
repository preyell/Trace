package com.sybyl.trace.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sybyl.trace.audit.AppAuditService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserCreatedListener {

	private final JavaMailSender mailSender;
	private final String baseUrl;

	public UserCreatedListener(JavaMailSender mailSender, @Value("${app.public-base-url}") String baseUrl,
			AppAuditService appAuditService) {
		this.mailSender = mailSender;
		this.baseUrl = baseUrl;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserCreated(UserCreatedEvent e) {
		try {
			

			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setTo(e.email());
			msg.setSubject("Welcome to Trace application!");
			msg.setText("""
					Hi %s,

					Your Trace account has been created.

					To login to the application, please click on the link below:
					%s


					— Trace Team
					""".formatted(e.firstName(), baseUrl));

			mailSender.send(msg);

			log.info("Welcome email sent: userId={}, email={}", e.userId(), e.email());

		} catch (Exception ex) {
			log.error("Welcome email failed: userId={}, email={}", e.userId(), e.email(), ex);

		}
	}
}
