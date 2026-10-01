package com.yanapaderina.rbot.schedule.internal.app;

import com.yanapaderina.rbot.schedule.BusyTime;
import com.yanapaderina.rbot.schedule.TimeRange;
import com.yanapaderina.rbot.schedule.internal.data.PracticeSettingsDao;
import com.yanapaderina.rbot.schedule.internal.data.ScheduleDayDao;
import com.yanapaderina.rbot.schedule.internal.data.SessionTypeDao;
import com.yanapaderina.rbot.schedule.internal.data.WorkIntervalDao;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-02, ADR-0003, REQ-CODE-DESIGN-003
@Service
public class SlotPreview {

  private final PracticeSettingsDao settings;
  private final WorkIntervalDao intervals;
  private final ScheduleDayDao days;
  private final SessionTypeDao types;
  private final ObjectProvider<BusyTime> busy;
  private final Clock clock;

  SlotPreview(PracticeSettingsDao settings, WorkIntervalDao intervals, ScheduleDayDao days, SessionTypeDao types,
    ObjectProvider<BusyTime> busy, Clock clock) {
    this.settings = settings;
    this.intervals = intervals;
    this.days = days;
    this.types = types;
    this.busy = busy;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<TimeRange> free(UUID typeId, LocalDate from, LocalDate to) {
    ScheduleRules.range(from, to);
    PracticeSettings practice = ScheduleRows.settings(settings.find());
    SessionType type = types.find(typeId).map(ScheduleRows::type)
      .orElseThrow(() -> new ScheduleRefused(ScheduleRefused.TYPE_MISSING, "Типа сессии нет"));
    TimeRange window = new TimeRange(ZonedDateTime.of(from.atStartOfDay(), practice.zone()).toInstant(),
      ZonedDateTime.of(to.plusDays(1).atStartOfDay(), practice.zone()).toInstant());
    List<TimeRange> taken = busy.orderedStream().flatMap(source -> source.busy(window).stream()).toList();
    return new SlotCalendar(practice, ScheduleRows.week(intervals.list()), ScheduleRows.days(days.between(from, to)), taken)
      .free(from, to, type.duration(), clock.instant());
  }
}
