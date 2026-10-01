package com.yanapaderina.rbot.telegram.internal.web;

import com.yanapaderina.rbot.access.Roles;
import com.yanapaderina.rbot.telegram.internal.app.InitData;
import com.yanapaderina.rbot.telegram.internal.app.TelegramSettings;
import io.github.apocarteres.platform.auth.ExternalIdentity;
import io.github.apocarteres.platform.auth.RequestAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// MVP-04, MVP-08, RBOT-FEAT-009, ADR-0002, REQ-AUTH-037, REQ-AUTH-038, REQ-AUTH-039
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TelegramSettings.class)
class TelegramAuthentication {

  static final String WEBHOOK = "/api/tg/webhook";
  static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";
  static final String INIT_DATA_HEADER = "X-Telegram-Init-Data";

  @Bean
  RequestAuthenticator telegramWebhook(TelegramSettings settings) {
    return new RequestAuthenticator() {
      @Override
      public Set<String> paths() {
        return Set.of(WEBHOOK);
      }

      @Override
      public Optional<ExternalIdentity> authenticate(HttpServletRequest request) {
        String given = request.getHeader(SECRET_HEADER);
        if (settings.webhookSecret().isEmpty() || given == null || !MessageDigest.isEqual(
          settings.webhookSecret().getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8))) {
          return Optional.empty();
        }
        return Optional.of(new ExternalIdentity("telegram-server", "bot", Set.of(Roles.TELEGRAM_SERVER)));
      }
    };
  }

  @Bean
  RequestAuthenticator telegramMiniApp(TelegramSettings settings, Clock clock) {
    return new RequestAuthenticator() {
      @Override
      public Set<String> paths() {
        return Set.of("/api/miniapp/**");
      }

      @Override
      public Optional<ExternalIdentity> authenticate(HttpServletRequest request) {
        return InitData.verify(request.getHeader(INIT_DATA_HEADER), settings.botToken(), clock.instant(), settings.initDataTtl())
          .map(user -> new ExternalIdentity("telegram", String.valueOf(user), Set.of(Roles.TELEGRAM_CLIENT)));
      }
    };
  }
}
