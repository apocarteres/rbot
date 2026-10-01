package com.yanapaderina.rbot.schedule.internal.app;

import com.yanapaderina.rbot.schedule.Availability;
import com.yanapaderina.rbot.schedule.BookingTerms;
import com.yanapaderina.rbot.schedule.BusyTime;
import com.yanapaderina.rbot.schedule.SessionType;
import com.yanapaderina.rbot.schedule.TimeRange;
import com.yanapaderina.rbot.schedule.internal.data.PracticeSettingsDao;
import com.yanapaderina.rbot.schedule.internal.data.ScheduleDayDao;
import com.yanapaderina.rbot.schedule.internal.data.SessionTypeDao;
import com.yanapaderina.rbot.schedule.internal.data.WorkIntervalDao;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-02, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-016, RBOT-FEAT-017, ADR-0003, REQ-CODE-DESIGN-003
@Service
public class SlotPreview implements Availability {

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

  @Override
  @Transactional(readOnly = true)
  public ZoneId zone(UUID practitioner) {
    return ScheduleRows.settings(settings.find(practitioner)).zone();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<BookingTerms> terms(UUID practitioner) {
    PracticeSettings practice = ScheduleRows.settings(settings.find(practitioner));
    if (!practice.complete()) {
      return Optional.empty();
    }
    return Optional.of(new BookingTerms(practice.zone(), practice.lead().orElseThrow(), practice.horizonDays().orElseThrow()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SessionType> types(UUID practitioner) {
    return types.list(practitioner).stream().map(ScheduleRows::type).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SessionType> everyType(UUID practitioner) {
    return types.listAll(practitioner).stream().map(ScheduleRows::type).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, String> names(Collection<UUID> practitioners) {
    return settings.names(practitioners);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TimeRange> free(UUID practitioner, UUID typeId, LocalDate from, LocalDate to) {
    ScheduleRules.range(from, to);
    PracticeSettings practice = ScheduleRows.settings(settings.find(practitioner));
    SessionType type = types.find(practitioner, typeId).map(ScheduleRows::type)
      .orElseThrow(() -> new ScheduleRefused(ScheduleRefused.TYPE_MISSING, "Типа сессии нет"));
    TimeRange window = new TimeRange(ZonedDateTime.of(from.atStartOfDay(), practice.zone()).toInstant(),
      ZonedDateTime.of(to.plusDays(1).atStartOfDay(), practice.zone()).toInstant());
    List<TimeRange> taken = busy.orderedStream().flatMap(source -> source.busy(practitioner, window).stream()).toList();
    return new SlotCalendar(practice, ScheduleRows.week(intervals.list(practitioner)),
      ScheduleRows.days(days.between(practitioner, from, to)), taken)
      .free(from, to, type.id(), type.duration(), type.buffer(), clock.instant());
  }
}
