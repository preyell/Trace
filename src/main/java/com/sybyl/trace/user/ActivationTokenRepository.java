package com.sybyl.trace.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
  Optional<ActivationToken> findByToken(String token);
  void deleteByUser(AppUser user);
}
