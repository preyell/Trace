package com.sybyl.trace.user;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserCreatedListener {
	private final JavaMailSender mailSender;
	private final String baseUrl;

	public UserCreatedListener(JavaMailSender mailSender, @Value("${app.public-base-url}") String baseUrl) {
		this.mailSender = mailSender;
		this.baseUrl = baseUrl;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserCreated(UserCreatedEvent e) {
		try {
			String link = baseUrl + "/activate?token=" + e.token();
			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setTo(e.email());
			msg.setSubject("Activate your Trace account");
			msg.setText("""
					Hi %s,

					Your Trace account has been created.

					To activate and set your password, please click the link below:
					%s

					This link expires in 48 hours.

					— Trace Team
					""".formatted(e.firstName(), link));
			mailSender.send(msg);
		} catch (Exception ex) {
			//log.error("Activation email failed for userId={} email={}", event.userId(), event.email(), e);
			//activationService.markEmailForRetry(e.userId(), ex.getMessage());
		}
	}
}
