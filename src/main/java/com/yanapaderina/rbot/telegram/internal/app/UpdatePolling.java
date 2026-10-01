package com.yanapaderina.rbot.telegram.internal.app;

import io.github.apocarteres.platform.persistence.JobLock;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

// MVP-04, RBOT-FEAT-009, ADR-0001, REQ-DEPLOYMENT-028
@Component
class UpdatePolling {

  static final String LOCK = "telegram-polling";
  static final int WAIT_SECONDS = 25;

  private final TelegramSettings settings;
  private final BotGateway bot;
  private final UpdateHandler handler;
  private final JobLock lock;
  private long offset;

  UpdatePolling(TelegramSettings settings, BotGateway bot, UpdateHandler handler, JobLock lock) {
    this.settings = settings;
    this.bot = bot;
    this.handler = handler;
    this.lock = lock;
  }

  @Scheduled(fixedDelayString = "PT1S", initialDelayString = "PT5S")
  void poll() {
    if (!settings.configured() || !settings.polling() || !lock.claim(LOCK, Duration.ofSeconds(WAIT_SECONDS * 3L))) {
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
      lock.release(LOCK);
    }
  }
}
