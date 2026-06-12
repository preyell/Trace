package com.sybyl.trace.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpService {
  private static final SecureRandom RNG = new SecureRandom();

  public String generateCode() {
    int n = RNG.nextInt(1_000_000); // 0..999999
    return String.format("%06d", n);
  }
}
