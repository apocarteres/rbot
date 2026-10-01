package com.yanapaderina.rbot.schedule.internal.web;

import com.yanapaderina.rbot.schedule.SessionFormat;
import com.yanapaderina.rbot.schedule.SessionType;
import com.yanapaderina.rbot.schedule.TimeRange;
import com.yanapaderina.rbot.schedule.internal.app.DayInterval;
import com.yanapaderina.rbot.schedule.internal.app.PracticeSettings;
import com.yanapaderina.rbot.schedule.internal.app.ScheduleDay;
import com.yanapaderina.rbot.schedule.internal.app.WeekTemplate;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// MVP-02, RBOT-FEAT-004, REQ-CODE-DESIGN-005
final class Views {

  private Views() {
  }

  record Interval(@NotNull LocalTime start, @NotNull LocalTime end, List<UUID> types) {

    static Interval of(DayInterval interval) {
      return new Interval(interval.start(), interval.end(), interval.types().stream().sorted().toList());
    }

    DayInterval domain() {
      return new DayInterval(start, end, types == null ? Set.of() : Set.copyOf(types));
    }
  }

  record Settings(String zone, Integer leadMinutes, Integer horizonDays, Integer slotStepMinutes, Integer bufferMinutes,
    boolean complete) {

    static Settings of(PracticeSettings settings) {
      return new Settings(settings.zone().getId(), minutes(settings.lead()), settings.horizonDays().orElse(null),
        minutes(settings.step()), minutes(settings.buffer()), settings.complete());
    }

    private static Integer minutes(Optional<Duration> value) {
      return value.map(duration -> Math.toIntExact(duration.toMinutes())).orElse(null);
    }
  }

  record SettingsRequest(@NotNull String zone, Integer leadMinutes, Integer horizonDays, Integer slotStepMinutes,
    Integer bufferMinutes) {

    PracticeSettings domain(ZoneId zoneId) {
      return new PracticeSettings(zoneId, duration(leadMinutes), Optional.ofNullable(horizonDays), duration(slotStepMinutes),
        duration(bufferMinutes));
    }

    private static Optional<Duration> duration(Integer minutes) {
      return Optional.ofNullable(minutes).map(Duration::ofMinutes);
    }
  }

  record Weekday(int weekday, List<Interval> intervals) {
  }

  static List<Weekday> week(WeekTemplate week) {
    return Arrays.stream(DayOfWeek.values())
      .map(day -> new Weekday(day.getValue(), week.of(day).stream().map(Interval::of).toList()))
      .toList();
  }

  record Hours(@NotNull List<Interval> intervals) {
  }

  record Day(LocalDate date, boolean closed, String note, List<Interval> intervals) {

    static Day of(ScheduleDay day) {
      return new Day(day.day(), day.closed(), day.note(), day.intervals().stream().map(Interval::of).toList());
    }
  }

  record DayRequest(boolean closed, String note, @NotNull List<Interval> intervals) {
  }

  record ClosedRange(@NotNull LocalDate from, @NotNull LocalDate to, String note) {
  }

  record Closed(int days) {
  }

  record Type(UUID id, String title, int durationMinutes, BigDecimal price, SessionFormat format, boolean firstVisit,
    boolean active) {

    static Type of(SessionType type) {
      return new Type(type.id(), type.title(), Math.toIntExact(type.duration().toMinutes()), type.price(), type.format(),
        type.firstVisit(), type.active());
    }
  }

  record TypeRequest(@NotNull String title, int durationMinutes, @NotNull BigDecimal price, @NotNull SessionFormat format,
    boolean firstVisit, boolean active) {

    SessionType domain(UUID id) {
      return new SessionType(id, title, Duration.ofMinutes(durationMinutes), price, format, firstVisit, active);
    }
  }

  record Slot(Instant start, Instant end) {

    static Slot of(TimeRange range) {
      return new Slot(range.start(), range.end());
    }
  }
}
