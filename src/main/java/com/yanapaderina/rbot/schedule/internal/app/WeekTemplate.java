package com.yanapaderina.rbot.schedule.internal.app;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// MVP-02, ADR-0003
public record WeekTemplate(Map<DayOfWeek, List<DayInterval>> days) {

  public WeekTemplate {
    Map<DayOfWeek, List<DayInterval>> copy = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek day : DayOfWeek.values()) {
      copy.put(day, List.copyOf(days.getOrDefault(day, List.of())));
    }
    days = Map.copyOf(copy);
  }

  public List<DayInterval> of(DayOfWeek day) {
    return days.get(day);
  }
}
