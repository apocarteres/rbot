package com.yanapaderina.rbot.schedule.internal.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yanapaderina.rbot.schedule.TimeRange;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MVP-02, ADR-0003
class SlotCalendarTest {

  private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
  private static final Instant SUNDAY_NOON = Instant.parse("2026-10-04T09:00:00Z");
  private static final Duration HOUR = Duration.ofHours(1);

  private static PracticeSettings settings(Duration lead, int horizonDays, Duration step, Duration buffer) {
    return new PracticeSettings(MOSCOW, Optional.of(lead), Optional.of(horizonDays), Optional.of(step), Optional.of(buffer));
  }

  private static WeekTemplate mondays(String start, String end) {
    return new WeekTemplate(Map.of(DayOfWeek.MONDAY, List.of(new DayInterval(LocalTime.parse(start), LocalTime.parse(end)))));
  }

  private static List<Instant> starts(List<TimeRange> slots) {
    return slots.stream().map(TimeRange::start).toList();
  }

  @Test
  @DisplayName("Шаблон «10:00 Europe/Moscow» даёт мгновение 07:00Z")
  void templateIsInPracticeZone() {
    List<TimeRange> slots = new SlotCalendar(settings(Duration.ZERO, 30, HOUR, Duration.ZERO), mondays("10:00", "11:00"), Map.of(),
      List.of()).free(MONDAY, MONDAY, HOUR, SUNDAY_NOON);
    assertThat(slots).containsExactly(new TimeRange(Instant.parse("2026-10-05T07:00:00Z"), Instant.parse("2026-10-05T08:00:00Z")));
  }

  @Test
  @DisplayName("Слоты идут с шагом; слот с перерывом, не помещающийся в конец интервала, не выдаётся")
  void stepAndBufferFitTheInterval() {
    List<TimeRange> slots = new SlotCalendar(settings(Duration.ZERO, 30, Duration.ofMinutes(30), Duration.ofMinutes(10)),
      mondays("10:00", "12:00"), Map.of(), List.of()).free(MONDAY, MONDAY, HOUR, SUNDAY_NOON);
    assertThat(starts(slots)).containsExactly(Instant.parse("2026-10-05T07:00:00Z"), Instant.parse("2026-10-05T07:30:00Z"));
  }

  @Test
  @DisplayName("Минимальный срок записи отсекает ближайшие слоты")
  void leadTimeCutsEarlySlots() {
    Instant mondayMorning = Instant.parse("2026-10-05T05:00:00Z");
    List<TimeRange> slots = new SlotCalendar(settings(Duration.ofHours(3), 30, HOUR, Duration.ZERO), mondays("10:00", "13:00"),
      Map.of(), List.of()).free(MONDAY, MONDAY, HOUR, mondayMorning);
    assertThat(starts(slots)).containsExactly(Instant.parse("2026-10-05T08:00:00Z"), Instant.parse("2026-10-05T09:00:00Z"));
  }

  @Test
  @DisplayName("Горизонт записи отсекает дальние слоты")
  void horizonCutsLateSlots() {
    List<TimeRange> slots = new SlotCalendar(settings(Duration.ZERO, 7, HOUR, Duration.ZERO), mondays("10:00", "11:00"), Map.of(),
      List.of()).free(MONDAY, MONDAY.plusWeeks(2), HOUR, SUNDAY_NOON);
    assertThat(starts(slots)).containsExactly(Instant.parse("2026-10-05T07:00:00Z"));
  }

  @Test
  @DisplayName("Закрытый день убирает слоты, особые часы заменяют шаблон")
  void overridesWin() {
    Map<LocalDate, ScheduleDay> days = Map.of(
      MONDAY, new ScheduleDay(MONDAY, true, "отпуск", List.of()),
      MONDAY.plusWeeks(1), new ScheduleDay(MONDAY.plusWeeks(1), false, null,
        List.of(new DayInterval(LocalTime.parse("15:00"), LocalTime.parse("16:00")))));
    List<TimeRange> slots = new SlotCalendar(settings(Duration.ZERO, 30, HOUR, Duration.ZERO), mondays("10:00", "11:00"), days,
      List.of()).free(MONDAY, MONDAY.plusWeeks(1), HOUR, SUNDAY_NOON);
    assertThat(starts(slots)).containsExactly(Instant.parse("2026-10-12T12:00:00Z"));
  }

  @Test
  @DisplayName("Занятое время вместе с перерывом не выдаётся")
  void busyTimeIsSkipped() {
    TimeRange booked = new TimeRange(Instant.parse("2026-10-05T07:00:00Z"), Instant.parse("2026-10-05T08:10:00Z"));
    List<TimeRange> slots = new SlotCalendar(settings(Duration.ZERO, 30, Duration.ofMinutes(30), Duration.ZERO),
      mondays("10:00", "12:30"), Map.of(), List.of(booked)).free(MONDAY, MONDAY, HOUR, SUNDAY_NOON);
    assertThat(starts(slots)).containsExactly(Instant.parse("2026-10-05T08:30:00Z"));
  }

  @Test
  @DisplayName("Без параметров записи слотов нет: settings-incomplete")
  void incompleteSettingsAreRefused() {
    PracticeSettings empty = new PracticeSettings(MOSCOW, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    assertThatThrownBy(() -> new SlotCalendar(empty, mondays("10:00", "11:00"), Map.of(), List.of()))
      .isInstanceOf(ScheduleRefused.class)
      .extracting(failure -> ((ScheduleRefused) failure).code().value()).isEqualTo("settings-incomplete");
  }

  @Test
  @DisplayName("Пересекающиеся промежутки дня отвергаются")
  void overlappingIntervalsAreRefused() {
    List<DayInterval> hours = List.of(new DayInterval(LocalTime.parse("10:00"), LocalTime.parse("12:00")),
      new DayInterval(LocalTime.parse("11:00"), LocalTime.parse("13:00")));
    assertThatThrownBy(() -> ScheduleRules.disjoint(hours)).isInstanceOf(ScheduleRefused.class);
  }
}
