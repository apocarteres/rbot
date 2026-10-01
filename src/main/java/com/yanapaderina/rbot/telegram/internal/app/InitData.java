package com.yanapaderina.rbot.telegram.internal.app;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.ObjectMapper;

// MVP-08, RBOT-FEAT-009, ADR-0002, REQ-AUTH-037, REQ-CODE-DESIGN-003
public final class InitData {

  private static final ObjectMapper JSON = new ObjectMapper();

  private InitData() {
  }

  public static Optional<Long> verify(String raw, String botToken, Instant now, Duration ttl) {
    if (raw == null || raw.isBlank() || botToken.isBlank()) {
      return Optional.empty();
    }
    Map<String, String> fields = new TreeMap<>();
    for (String pair : raw.split("&", -1)) {
      int split = pair.indexOf('=');
      if (split <= 0) {
        return Optional.empty();
      }
      fields.put(URLDecoder.decode(pair.substring(0, split), StandardCharsets.UTF_8),
        URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8));
    }
    String hash = fields.remove("hash");
    String authDate = fields.get("auth_date");
    String user = fields.get("user");
    if (hash == null || authDate == null || user == null) {
      return Optional.empty();
    }
    String checked = fields.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("\n"));
    byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
    byte[] expected = hmac(secret, checked.getBytes(StandardCharsets.UTF_8));
    byte[] given;
    try {
      given = HexFormat.of().parseHex(hash);
    } catch (IllegalArgumentException malformed) {
      return Optional.empty();
    }
    if (!MessageDigest.isEqual(expected, given)) {
      return Optional.empty();
    }
    Instant issued;
    try {
      issued = Instant.ofEpochSecond(Long.parseLong(authDate));
    } catch (NumberFormatException malformed) {
      return Optional.empty();
    }
    if (issued.isAfter(now.plusSeconds(60)) || issued.plus(ttl).isBefore(now)) {
      return Optional.empty();
    }
    long id = JSON.readTree(user).path("id").asLong(0);
    return id > 0 ? Optional.of(id) : Optional.empty();
  }

  static byte[] hmac(byte[] key, byte[] message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return mac.doFinal(message);
    } catch (GeneralSecurityException absent) {
      throw new IllegalStateException(absent);
    }
  }
}
