package com.yanapaderina.rbot.telegram.internal.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanapaderina.rbot.telegram.internal.data.PollingLeaseDao;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// MVP-04, RBOT-FEAT-009, RBOT-OPS-018, REQ-DEPLOYMENT-028
class UpdatePollingTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final TelegramSettings SETTINGS = new TelegramSettings("1:token", "bot", "", null, "", "", null, true);

  private final BotGateway bot = mock(BotGateway.class);
  private final UpdateHandler handler = mock(UpdateHandler.class);
  private final PollingLeaseDao lease = mock(PollingLeaseDao.class);
  private final UpdatePolling polling = new UpdatePolling(SETTINGS, bot, handler, lease,
    Clock.fixed(Instant.parse("2026-10-02T06:00:00Z"), ZoneOffset.UTC), new SimpleMeterRegistry());

  @Test
  @DisplayName("Без аренды экземпляр Telegram не опрашивает")
  void secondInstanceSkips() {
    when(lease.claim(anyString(), any(Instant.class), any(Instant.class), any(Instant.class))).thenReturn(false);
    polling.poll();
    verify(bot, never()).updates(anyLong(), anyInt());
  }

  @Test
  @DisplayName("Держатель аренды обрабатывает обновления и сдвигает offset за последнее")
  void holderHandlesAndAdvances() {
    JsonNode first = JSON.readTree("{\"update_id\":41}");
    JsonNode second = JSON.readTree("{\"update_id\":42}");
    when(lease.claim(anyString(), any(Instant.class), any(Instant.class), any(Instant.class))).thenReturn(true);
    when(bot.updates(0, UpdatePolling.WAIT_SECONDS)).thenReturn(List.of(first, second));
    polling.poll();
    polling.poll();
    verify(handler).handle(first);
    verify(handler).handle(second);
    verify(bot).updates(43, UpdatePolling.WAIT_SECONDS);
  }
}
