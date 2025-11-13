// com.sybyl.trace.auth.PasswordResetTokenRepository.java
package com.sybyl.trace.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	// PasswordResetTokenRepository.java
	@Query("""
	  select t from PasswordResetToken t
	  join fetch t.user u
	  where t.token = :token
	    and t.usedAt is null
	    and t.expiresAt > :now
	""")
	Optional<PasswordResetToken> findActiveByTokenFetchUser(@Param("token") String token,
	                                                        @Param("now") java.time.Instant now);
}
