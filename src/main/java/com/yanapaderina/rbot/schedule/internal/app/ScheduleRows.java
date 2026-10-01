package com.yanapaderina.rbot.schedule.internal.app;

import com.yanapaderina.rbot.schedule.SessionFormat;
import com.yanapaderina.rbot.schedule.SessionType;
import com.yanapaderina.rbot.schedule.internal.data.DayRow;
import com.yanapaderina.rbot.schedule.internal.data.IntervalRow;
import com.yanapaderina.rbot.schedule.internal.data.SessionTypeRow;
import com.yanapaderina.rbot.schedule.internal.data.SettingsRow;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// MVP-02, RBOT-FEAT-004
final class ScheduleRows {

  private ScheduleRows() {
  }

  static PracticeSettings settings(SettingsRow row) {
    return new PracticeSettings(ZoneId.of(row.zone()), minutes(row.leadMinutes()), Optional.ofNullable(row.horizonDays()),
      minutes(row.slotStepMinutes()), minutes(row.bufferMinutes()));
  }

  static SettingsRow row(PracticeSettings settings) {
    return new SettingsRow(settings.zone().getId(), toMinutes(settings.lead()), settings.horizonDays().orElse(null),
      toMinutes(settings.step()), toMinutes(settings.buffer()));
  }

  static WeekTemplate week(List<IntervalRow> rows) {
    Map<DayOfWeek, List<DayInterval>> days = new EnumMap<>(DayOfWeek.class);
    for (IntervalRow row : rows) {
      days.computeIfAbsent(DayOfWeek.of(row.weekday()), day -> new ArrayList<>()).add(new DayInterval(row.starts(), row.ends(), Set.copyOf(row.types())));
    }
    return new WeekTemplate(days);
  }

  static Map<LocalDate, ScheduleDay> days(List<DayRow> rows) {
    Map<LocalDate, List<DayRow>> grouped = new LinkedHashMap<>();
    for (DayRow row : rows) {
      grouped.computeIfAbsent(row.day(), day -> new ArrayList<>()).add(row);
    }
    Map<LocalDate, ScheduleDay> days = new LinkedHashMap<>();
    grouped.forEach((day, group) -> days.put(day, new ScheduleDay(day, group.getFirst().closed(), group.getFirst().note(),
      group.stream().filter(row -> row.starts() != null).map(row -> new DayInterval(row.starts(), row.ends(), Set.copyOf(row.types())))
        .toList())));
    return days;
  }

  static SessionType type(SessionTypeRow row) {
    return new SessionType(row.id(), row.title(), Duration.ofMinutes(row.durationMinutes()), row.price(),
      SessionFormat.valueOf(row.format()), row.firstVisit(), row.active());
  }

  static SessionTypeRow row(SessionType type) {
    return new SessionTypeRow(type.id(), type.title(), Math.toIntExact(type.duration().toMinutes()), type.price(),
      type.format().name(), type.firstVisit(), type.active());
  }

  private static Optional<Duration> minutes(Integer value) {
    return Optional.ofNullable(value).map(Duration::ofMinutes);
  }

  private static Integer toMinutes(Optional<Duration> value) {
    return value.map(duration -> Math.toIntExact(duration.toMinutes())).orElse(null);
  }
}
