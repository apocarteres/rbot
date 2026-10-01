package com.yanapaderina.rbot.booking.internal.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MVP-05, RBOT-FEAT-002, ADR-0004
class CancelDeadlineTest {

  private static final Instant START = Instant.parse("2026-10-05T07:00:00Z");
  private static final Duration DAY = Duration.ofHours(24);

  @Test
  @DisplayName("Отмена не позже минимального срока записи разрешена, в последний момент срока — тоже")
  void allowsUntilLead() {
    assertThat(CancelDeadline.allows(START, START.minus(DAY).minusSeconds(1), DAY)).isTrue();
    assertThat(CancelDeadline.allows(START, START.minus(DAY), DAY)).isTrue();
  }

  @Test
  @DisplayName("Внутри минимального срока и после начала отмена запрещена")
  void refusesInsideLead() {
    assertThat(CancelDeadline.allows(START, START.minus(DAY).plusSeconds(1), DAY)).isFalse();
    assertThat(CancelDeadline.allows(START, START.plusSeconds(1), Duration.ZERO)).isFalse();
  }
}
