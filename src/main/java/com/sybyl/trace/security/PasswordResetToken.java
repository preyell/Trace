// com.sybyl.trace.auth.PasswordResetToken.java
package com.sybyl.trace.security;

import java.time.Instant;

import com.sybyl.trace.user.AppUser;
import jakarta.persistence.*;
import lombok.Getter; 
import lombok.Setter; 


@Entity
@Getter
@Setter
@Table(name = "password_reset_token")
public class PasswordResetToken {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Column(nullable = false, unique = true, length = 100)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;
}
