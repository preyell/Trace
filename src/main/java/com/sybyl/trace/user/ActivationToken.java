package com.sybyl.trace.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activation_token")
public class ActivationToken {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Column(nullable = false, unique = true, length = 120)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  // getters/setters
  public Long getId() { return id; }
  public AppUser getUser() { return user; }
  public void setUser(AppUser user) { this.user = user; }
  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
