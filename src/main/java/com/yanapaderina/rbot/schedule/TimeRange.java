package com.yanapaderina.rbot.schedule;

import java.time.Instant;

// MVP-02, ADR-0003
public record TimeRange(Instant start, Instant end) {

  public TimeRange {
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("Начало промежутка " + start + " не раньше конца " + end);
    }
  }

  public boolean overlaps(TimeRange other) {
    return start.isBefore(other.end) && other.start.isBefore(end);
  }
}
