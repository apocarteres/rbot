package com.yanapaderina.rbot.schedule.internal.app;

import com.yanapaderina.rbot.schedule.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// MVP-02, ADR-0003, REQ-CODE-DESIGN-003
public final class SlotCalendar {

  private final PracticeSettings settings;
  private final WeekTemplate week;
  private final Map<LocalDate, ScheduleDay> days;
  private final List<TimeRange> busy;

  public SlotCalendar(PracticeSettings settings, WeekTemplate week, Map<LocalDate, ScheduleDay> days, List<TimeRange> busy) {
    if (!settings.complete()) {
      throw new ScheduleRefused(ScheduleRefused.INCOMPLETE, "Параметры записи не заданы");
    }
    this.settings = settings;
    this.week = week;
    this.days = Map.copyOf(days);
    this.busy = List.copyOf(busy);
  }

  public List<TimeRange> free(LocalDate from, LocalDate to, Duration duration, Instant now) {
    Instant earliest = now.plus(settings.lead().orElseThrow());
    Instant latest = now.plus(Duration.ofDays(settings.horizonDays().orElseThrow()));
    Duration occupied = duration.plus(settings.buffer().orElseThrow());
    Duration step = settings.step().orElseThrow();
    List<TimeRange> slots = new ArrayList<>();
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      for (DayInterval hours : hoursOf(day)) {
        Instant intervalEnd = at(day, hours, false);
        for (Instant start = at(day, hours, true); !start.plus(occupied).isAfter(intervalEnd); start = start.plus(step)) {
          TimeRange taken = new TimeRange(start, start.plus(occupied));
          if (!start.isBefore(earliest) && !start.isAfter(latest) && busy.stream().noneMatch(taken::overlaps)) {
            slots.add(new TimeRange(start, start.plus(duration)));
          }
        }
      }
    }
    return List.copyOf(slots);
  }

  private List<DayInterval> hoursOf(LocalDate day) {
    ScheduleDay special = days.get(day);
    return special != null ? special.intervals() : week.of(day.getDayOfWeek());
  }

  private Instant at(LocalDate day, DayInterval hours, boolean start) {
    return ZonedDateTime.of(day, start ? hours.start() : hours.end(), settings.zone()).toInstant();
  }
}
