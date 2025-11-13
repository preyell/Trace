package com.sybyl.trace.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivationService {
  private final ActivationTokenRepository tokens;
  private final AppUserRepository users;
  private final PasswordEncoder encoder;
  public ActivationService(ActivationTokenRepository tokens, AppUserRepository users, PasswordEncoder encoder) {
    this.tokens = tokens; this.users = users; this.encoder = encoder;
  }

  @Transactional
  public ActivationToken createTokenFor(AppUser user) {
    tokens.deleteByUser(user); // one active token per user
    ActivationToken t = new ActivationToken();
    t.setUser(user);
    t.setToken(Tokens.urlSafe(32));
    t.setExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
    return tokens.save(t);
  }

  @Transactional
  public void activate(String token, String rawPassword) {
    ActivationToken t = tokens.findByToken(token)
        .orElseThrow(() -> new IllegalArgumentException("Invalid or used activation link"));
    if (t.getExpiresAt().isBefore(Instant.now()))
      throw new IllegalArgumentException("Activation link expired");

    AppUser u = t.getUser();
    u.setPassword(encoder.encode(rawPassword));
    u.setEnabled(true);
    users.save(u);
    tokens.deleteByUser(u); // consume & clear
  }
}
