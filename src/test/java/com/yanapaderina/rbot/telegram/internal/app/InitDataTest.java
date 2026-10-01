package com.yanapaderina.rbot.telegram.internal.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MVP-08, RBOT-FEAT-009, ADR-0002
class InitDataTest {

  private static final String TOKEN = "123456:test-token-not-real";
  private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");
  private static final Duration HOUR = Duration.ofHours(1);

  static String signed(String token, long user, Instant authDate) {
    Map<String, String> fields = new TreeMap<>();
    fields.put("auth_date", String.valueOf(authDate.getEpochSecond()));
    fields.put("query_id", "AAE-test");
    fields.put("user", "{\"id\":" + user + ",\"first_name\":\"Тест\"}");
    String checked = fields.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("\n"));
    byte[] secret = InitData.hmac("WebAppData".getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    String hash = HexFormat.of().formatHex(InitData.hmac(secret, checked.getBytes(StandardCharsets.UTF_8)));
    return fields.entrySet().stream().map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
      .collect(Collectors.joining("&")) + "&hash=" + hash;
  }

  @Test
  @DisplayName("Подписанные токеном бота данные дают идентификатор пользователя")
  void validSignatureGivesUser() {
    assertThat(InitData.verify(signed(TOKEN, 42, NOW.minusSeconds(30)), TOKEN, NOW, HOUR)).contains(42L);
  }

  @Test
  @DisplayName("Чужой токен, правка поля и устаревшая дата отвергаются")
  void forgedOrStaleDataIsRejected() {
    assertThat(InitData.verify(signed("999:other", 42, NOW), TOKEN, NOW, HOUR)).isEmpty();
    assertThat(InitData.verify(signed(TOKEN, 42, NOW).replace("%3A42", "%3A43"), TOKEN, NOW, HOUR)).isEmpty();
    assertThat(InitData.verify(signed(TOKEN, 42, NOW.minus(HOUR).minusSeconds(1)), TOKEN, NOW, HOUR)).isEmpty();
    assertThat(InitData.verify("hash=zz&auth_date=1", TOKEN, NOW, HOUR)).isEmpty();
    assertThat(InitData.verify(signed(TOKEN, 42, NOW), "", NOW, HOUR)).isEmpty();
  }
}
