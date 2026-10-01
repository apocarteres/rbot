package com.yanapaderina.rbot.telegram.internal.app;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// MVP-04, MVP-08, RBOT-FEAT-009, ADR-0002
@ConfigurationProperties("rbot.telegram")
public record TelegramSettings(String botToken, String botUsername, String webhookSecret, String apiBase, String proxy, String appUrl,
  Duration initDataTtl) {

  public TelegramSettings {
    botToken = botToken == null ? "" : botToken.trim();
    botUsername = botUsername == null ? "" : botUsername.trim();
    webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    apiBase = apiBase == null || apiBase.isBlank() ? "https://api.telegram.org" : apiBase.trim();
    proxy = proxy == null ? "" : proxy.trim();
    appUrl = appUrl == null ? "" : appUrl.trim();
    initDataTtl = initDataTtl == null ? Duration.ofHours(1) : initDataTtl;
  }

  public boolean configured() {
    return !botToken.isEmpty();
  }
}
