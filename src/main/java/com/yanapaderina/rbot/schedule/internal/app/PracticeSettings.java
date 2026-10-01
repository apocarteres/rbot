package com.yanapaderina.rbot.schedule.internal.app;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Optional;

// MVP-02, RBOT-FEAT-016, RBOT-FEAT-017, ADR-0003
public record PracticeSettings(ZoneId zone, Optional<Duration> lead, Optional<Integer> horizonDays, Optional<Duration> step,
  Optional<String> displayName) {

  public static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Moscow");

  public boolean complete() {
    return lead.isPresent() && horizonDays.isPresent() && step.isPresent();
  }
}
