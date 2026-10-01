package com.yanapaderina.rbot.schedule.internal.app;

import com.yanapaderina.rbot.schedule.internal.data.PracticeSettingsDao;
import com.yanapaderina.rbot.schedule.internal.data.ScheduleDayDao;
import com.yanapaderina.rbot.schedule.internal.data.SessionTypeDao;
import com.yanapaderina.rbot.schedule.internal.data.WorkIntervalDao;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-02, ADR-0003, REQ-DATA-ACCESS-003, REQ-CODE-DESIGN-004
@Service
public class ScheduleAdministration {

  private final PracticeSettingsDao settings;
  private final WorkIntervalDao intervals;
  private final ScheduleDayDao days;
  private final SessionTypeDao types;
  private final Clock clock;

  ScheduleAdministration(PracticeSettingsDao settings, WorkIntervalDao intervals, ScheduleDayDao days, SessionTypeDao types,
    Clock clock) {
    this.settings = settings;
    this.intervals = intervals;
    this.days = days;
    this.types = types;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PracticeSettings settings() {
    return ScheduleRows.settings(settings.find());
  }

  @Transactional
  public PracticeSettings changeSettings(PracticeSettings changed) {
    settings.update(ScheduleRows.row(ScheduleRules.settings(changed)), clock.instant());
    return changed;
  }

  @Transactional(readOnly = true)
  public WeekTemplate week() {
    return ScheduleRows.week(intervals.list());
  }

  @Transactional
  public List<DayInterval> replaceWeekday(DayOfWeek weekday, List<DayInterval> hours) {
    List<DayInterval> sorted = ScheduleRules.disjoint(hours);
    intervals.deleteWeekday(weekday.getValue());
    sorted.forEach(hour -> intervals.insert(UUID.randomUUID(), weekday.getValue(), hour.start(), hour.end()));
    return sorted;
  }

  @Transactional(readOnly = true)
  public Map<LocalDate, ScheduleDay> days(LocalDate from, LocalDate to) {
    ScheduleRules.range(from, to);
    return ScheduleRows.days(days.between(from, to));
  }

  @Transactional
  public ScheduleDay setDay(ScheduleDay day) {
    List<DayInterval> sorted = ScheduleRules.disjoint(day.intervals());
    days.upsert(day.day(), day.closed(), day.note());
    days.deleteIntervals(day.day());
    sorted.forEach(hour -> days.insertInterval(UUID.randomUUID(), day.day(), hour.start(), hour.end()));
    return new ScheduleDay(day.day(), day.closed(), day.note(), sorted);
  }

  @Transactional
  public int closeDays(LocalDate from, LocalDate to, String note) {
    ScheduleRules.range(from, to);
    int closed = 0;
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      setDay(new ScheduleDay(day, true, note, List.of()));
      closed++;
    }
    return closed;
  }

  @Transactional
  public void clearDay(LocalDate day) {
    days.delete(day);
  }

  @Transactional(readOnly = true)
  public List<SessionType> types() {
    return types.list().stream().map(ScheduleRows::type).toList();
  }

  @Transactional
  public SessionType createType(SessionType type) {
    SessionType created = ScheduleRules.type(new SessionType(UUID.randomUUID(), type.title().trim(), type.duration(), type.price(),
      type.format(), type.firstVisit(), type.active()));
    types.insert(ScheduleRows.row(created), clock.instant());
    return created;
  }

  @Transactional
  public SessionType changeType(SessionType type) {
    if (types.find(type.id()).isEmpty()) {
      throw new ScheduleRefused(ScheduleRefused.TYPE_MISSING, "Типа сессии нет");
    }
    SessionType changed = ScheduleRules.type(new SessionType(type.id(), type.title().trim(), type.duration(), type.price(),
      type.format(), type.firstVisit(), type.active()));
    types.update(ScheduleRows.row(changed));
    return changed;
  }
}
