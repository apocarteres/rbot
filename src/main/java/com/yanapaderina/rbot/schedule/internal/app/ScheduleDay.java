package com.yanapaderina.rbot.schedule.internal.app;

import java.time.LocalDate;
import java.util.List;

// MVP-02, ADR-0003
public record ScheduleDay(LocalDate day, boolean closed, String note, List<DayInterval> intervals) {

  public ScheduleDay {
    intervals = closed ? List.of() : List.copyOf(intervals);
  }
}
