package com.yanapaderina.rbot.schedule.internal.app;

import java.time.LocalTime;

// MVP-02, ADR-0003
public record DayInterval(LocalTime start, LocalTime end) {

  public DayInterval {
    if (!start.isBefore(end)) {
      throw new ScheduleRefused(ScheduleRefused.INTERVAL, "Начало " + start + " не раньше конца " + end);
    }
  }

  boolean overlaps(DayInterval other) {
    return start.isBefore(other.end) && other.start.isBefore(end);
  }
}
