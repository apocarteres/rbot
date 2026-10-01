package com.yanapaderina.rbot.schedule.internal.app;

import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

// MVP-02, RBOT-FEAT-004, RBOT-FEAT-008, ADR-0003
public record DayInterval(LocalTime start, LocalTime end, Set<UUID> types) {

  public DayInterval {
    if (!start.isBefore(end)) {
      throw new ScheduleRefused(ScheduleRefused.INTERVAL, "Начало " + start + " не раньше конца " + end);
    }
    types = types == null ? Set.of() : Set.copyOf(types);
  }

  public DayInterval(LocalTime start, LocalTime end) {
    this(start, end, Set.of());
  }

  boolean overlaps(DayInterval other) {
    return start.isBefore(other.end) && other.start.isBefore(end);
  }

  boolean serves(UUID type) {
    return types.contains(type);
  }
}
