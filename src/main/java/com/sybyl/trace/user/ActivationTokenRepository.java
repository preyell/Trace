package com.sybyl.trace.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
	@EntityGraph(attributePaths = "user")
	Optional<ActivationToken> findByToken(String token);

	void deleteByUserId(Long userId);
}
