package com.yanapaderina.rbot.clients.internal.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// MVP-03, RBOT-FEAT-009, ADR-0002
final class InviteTokens {

  private static final SecureRandom RANDOM = new SecureRandom();

  private InviteTokens() {
  }

  static String issue() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String hash(String token) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException absent) {
      throw new IllegalStateException(absent);
    }
  }
}
