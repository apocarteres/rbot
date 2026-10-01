package com.yanapaderina.rbot.telegram.internal.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.apocarteres.platform.persistence.JobLock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// MVP-04, RBOT-FEAT-009, REQ-DEPLOYMENT-028
class UpdatePollingTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final TelegramSettings SETTINGS = new TelegramSettings("1:token", "bot", "", null, "", "", null, true);

  private final BotGateway bot = mock(BotGateway.class);
  private final UpdateHandler handler = mock(UpdateHandler.class);
  private final JobLock lock = mock(JobLock.class);
  private final UpdatePolling polling = new UpdatePolling(SETTINGS, bot, handler, lock);

  @Test
  @DisplayName("Без замка экземпляр Telegram не опрашивает")
  void secondInstanceSkips() {
    when(lock.claim(eq(UpdatePolling.LOCK), any(Duration.class))).thenReturn(false);
    polling.poll();
    verify(bot, never()).updates(anyLong(), anyInt());
  }

  @Test
  @DisplayName("Держатель замка обрабатывает обновления и сдвигает offset за последнее")
  void holderHandlesAndAdvances() {
    JsonNode first = JSON.readTree("{\"update_id\":41}");
    JsonNode second = JSON.readTree("{\"update_id\":42}");
    when(lock.claim(eq(UpdatePolling.LOCK), any(Duration.class))).thenReturn(true);
    when(bot.updates(0, UpdatePolling.WAIT_SECONDS)).thenReturn(List.of(first, second));
    polling.poll();
    polling.poll();
    verify(handler).handle(first);
    verify(handler).handle(second);
    verify(bot).updates(43, UpdatePolling.WAIT_SECONDS);
  }
}
