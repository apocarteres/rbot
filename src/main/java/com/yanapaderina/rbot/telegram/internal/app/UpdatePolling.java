package com.yanapaderina.rbot.telegram.internal.app;

import com.yanapaderina.rbot.telegram.internal.data.PollingLeaseDao;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

// MVP-04, RBOT-FEAT-009, RBOT-OPS-018, ADR-0001, REQ-DEPLOYMENT-028
@Component
class UpdatePolling {

  static final int WAIT_SECONDS = 25;
  static final Duration ABANDONED = Duration.ofSeconds(WAIT_SECONDS * 3L);

  private final TelegramSettings settings;
  private final BotGateway bot;
  private final UpdateHandler handler;
  private final PollingLeaseDao lease;
  private final Clock clock;
  private final String instance = "rbot-" + UUID.randomUUID();
  private final Instant started;
  private long offset;

  UpdatePolling(TelegramSettings settings, BotGateway bot, UpdateHandler handler, PollingLeaseDao lease, Clock clock) {
    this.settings = settings;
    this.bot = bot;
    this.handler = handler;
    this.lease = lease;
    this.clock = clock;
    this.started = clock.instant();
  }

  @Scheduled(fixedDelayString = "PT1S", initialDelayString = "PT5S")
  void poll() {
    if (!settings.configured() || !settings.polling()) {
      return;
    }
    Instant now = clock.instant();
    if (!lease.claim(instance, started, now, now.minus(ABANDONED))) {
      return;
    }
    List<JsonNode> updates = bot.updates(offset, WAIT_SECONDS);
    for (JsonNode update : updates) {
      handler.handle(update);
      offset = Math.max(offset, update.path("update_id").asLong(-1) + 1);
    }
  }

  @PreDestroy
  void stop() {
    if (settings.configured() && settings.polling()) {
      lease.release(instance);
    }
  }
}
