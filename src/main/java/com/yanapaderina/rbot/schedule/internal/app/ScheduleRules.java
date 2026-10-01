package com.yanapaderina.rbot.schedule.internal.app;

import com.yanapaderina.rbot.schedule.SessionType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// MVP-02, ADR-0003
final class ScheduleRules {

  static final int MAX_RANGE_DAYS = 366;

  private ScheduleRules() {
  }

  static List<DayInterval> disjoint(List<DayInterval> intervals) {
    List<DayInterval> sorted = intervals.stream().sorted(Comparator.comparing(DayInterval::start)).toList();
    for (int i = 1; i < sorted.size(); i++) {
      if (sorted.get(i - 1).overlaps(sorted.get(i))) {
        throw new ScheduleRefused(ScheduleRefused.OVERLAP, "Промежутки " + sorted.get(i - 1) + " и " + sorted.get(i) + " пересекаются");
      }
    }
    return sorted;
  }

  static void range(LocalDate from, LocalDate to) {
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
      throw new ScheduleRefused(ScheduleRefused.RANGE, "Диапазон " + from + "–" + to);
    }
  }

  static PracticeSettings settings(PracticeSettings settings) {
    within(settings.lead(), Duration.ZERO, Duration.ofDays(7), "минимальный срок записи");
    within(settings.step(), Duration.ofMinutes(5), Duration.ofHours(4), "шаг слотов");
    within(settings.buffer(), Duration.ZERO, Duration.ofHours(4), "перерыв");
    settings.horizonDays().filter(days -> days < 1 || days > 365).ifPresent(days -> {
      throw new ScheduleRefused(ScheduleRefused.SETTINGS, "Горизонт записи " + days + " дн.: от 1 до 365");
    });
    return settings;
  }

  static SessionType type(SessionType type) {
    if (type.title() == null || type.title().isBlank() || type.title().length() > 100) {
      throw new ScheduleRefused(ScheduleRefused.TYPE, "Название типа сессии — от 1 до 100 знаков");
    }
    if (type.duration().compareTo(Duration.ofMinutes(15)) < 0 || type.duration().compareTo(Duration.ofHours(8)) > 0) {
      throw new ScheduleRefused(ScheduleRefused.TYPE, "Длительность сессии — от 15 минут до 8 часов");
    }
    if (type.price().signum() < 0 || type.price().scale() > 2 || type.price().compareTo(new BigDecimal("9999999999.99")) > 0) {
      throw new ScheduleRefused(ScheduleRefused.TYPE, "Цена — неотрицательная, не больше двух знаков после запятой");
    }
    return type;
  }

  private static void within(Optional<Duration> value, Duration min, Duration max, String what) {
    value.filter(duration -> duration.compareTo(min) < 0 || duration.compareTo(max) > 0).ifPresent(duration -> {
      throw new ScheduleRefused(ScheduleRefused.SETTINGS, "Параметр «" + what + "» " + duration.toMinutes() + " мин вне границ");
    });
  }
}
