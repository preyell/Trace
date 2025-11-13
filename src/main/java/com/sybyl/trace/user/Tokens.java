package com.sybyl.trace.user;

import java.security.SecureRandom;
import java.util.Base64;

public final class Tokens {
  private static final SecureRandom RND = new SecureRandom();
  private Tokens() {}
  public static String urlSafe(int numBytes) {
    byte[] b = new byte[numBytes];
    RND.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }
}
