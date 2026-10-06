package com.yanapaderina.rbot.schedule.internal.app;

import com.yanapaderina.rbot.schedule.Availability;
import com.yanapaderina.rbot.schedule.BookingTerms;
import com.yanapaderina.rbot.schedule.BusyTime;
import com.yanapaderina.rbot.schedule.SessionType;
import com.yanapaderina.rbot.schedule.TimeRange;
import com.yanapaderina.rbot.schedule.internal.data.OpenSlotDao;
import com.yanapaderina.rbot.schedule.internal.data.PracticeSettingsDao;
import com.yanapaderina.rbot.schedule.internal.data.ScheduleDayDao;
import com.yanapaderina.rbot.schedule.internal.data.SessionTypeDao;
import com.yanapaderina.rbot.schedule.internal.data.WorkIntervalDao;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-02, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-016, RBOT-FEAT-017, RBOT-FEAT-021, ADR-0003, REQ-CODE-DESIGN-003
@Service
public class SlotPreview implements Availability {

  static final int MAX_OPENINGS = 2000;

  private final PracticeSettingsDao settings;
  private final WorkIntervalDao intervals;
  private final ScheduleDayDao days;
  private final SessionTypeDao types;
  private final OpenSlotDao opened;
  private final ObjectProvider<BusyTime> busy;
  private final Clock clock;

  SlotPreview(PracticeSettingsDao settings, WorkIntervalDao intervals, ScheduleDayDao days, SessionTypeDao types, OpenSlotDao opened,
    ObjectProvider<BusyTime> busy, Clock clock) {
    this.settings = settings;
    this.intervals = intervals;
    this.days = days;
    this.types = types;
    this.opened = opened;
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
    List<TimeRange> slots = proposed(practitioner, typeId, from, to);
    if (slots.isEmpty()) {
      return slots;
    }
    Set<Instant> open = new HashSet<>(opened.between(practitioner, slots.getFirst().start(), slots.getLast().start().plusSeconds(1)));
    return slots.stream().filter(slot -> open.contains(slot.start())).toList();
  }

  @Transactional(readOnly = true)
  public List<TimeRange> proposed(UUID practitioner, UUID typeId, LocalDate from, LocalDate to) {
    ScheduleRules.range(from, to);
    PracticeSettings practice = ScheduleRows.settings(settings.find(practitioner));
    SessionType type = types.find(practitioner, typeId).map(ScheduleRows::type)
      .orElseThrow(() -> new ScheduleRefused(ScheduleRefused.TYPE_MISSING, "Типа сессии нет"));
    return calendar(practitioner, practice, from, to, true).free(from, to, type.id(), type.duration(), type.buffer(), clock.instant());
  }

  @Transactional(readOnly = true)
  public List<Opening> openings(UUID practitioner, LocalDate from, LocalDate to) {
    ScheduleRules.range(from, to);
    PracticeSettings practice = ScheduleRows.settings(settings.find(practitioner));
    SlotCalendar every = calendar(practitioner, practice, from, to, false);
    SlotCalendar free = calendar(practitioner, practice, from, to, true);
    Instant now = clock.instant();
    Set<Instant> starts = new TreeSet<>();
    Set<Instant> available = new HashSet<>();
    for (SessionType type : types(practitioner).stream().filter(SessionType::active).toList()) {
      every.free(from, to, type.id(), type.duration(), type.buffer(), now).forEach(slot -> starts.add(slot.start()));
      free.free(from, to, type.id(), type.duration(), type.buffer(), now).forEach(slot -> available.add(slot.start()));
    }
    TimeRange window = window(practice, from, to);
    Set<Instant> open = new HashSet<>(opened.between(practitioner, window.start(), window.end()));
    return starts.stream().map(start -> new Opening(start, !available.contains(start) ? Opening.State.BUSY
      : open.contains(start) ? Opening.State.OPEN : Opening.State.CLOSED)).toList();
  }

  @Transactional
  public void changeOpenings(UUID practitioner, List<Instant> open, List<Instant> close) {
    PracticeSettings practice = ScheduleRows.settings(settings.find(practitioner));
    if (!practice.complete()) {
      throw new ScheduleRefused(ScheduleRefused.INCOMPLETE, "Параметры записи не заданы");
    }
    Instant now = clock.instant();
    Instant latest = now.plus(Duration.ofDays(practice.horizonDays().orElseThrow() + 1L));
    if (open.size() + close.size() > MAX_OPENINGS || open.stream().anyMatch(start -> start.isBefore(now) || start.isAfter(latest))) {
      throw new ScheduleRefused(ScheduleRefused.OPENING, "Открыть можно до " + MAX_OPENINGS + " мгновений в пределах горизонта записи");
    }
    close.forEach(start -> opened.close(practitioner, start));
    open.forEach(start -> opened.open(practitioner, start, now));
  }

  private SlotCalendar calendar(UUID practitioner, PracticeSettings practice, LocalDate from, LocalDate to, boolean withBusy) {
    TimeRange window = window(practice, from, to);
    List<TimeRange> taken = withBusy ? busy.orderedStream().flatMap(source -> source.busy(practitioner, window).stream()).toList()
      : List.of();
    return new SlotCalendar(practice, ScheduleRows.week(intervals.list(practitioner)), ScheduleRows.days(days.between(practitioner, from, to)),
      taken);
  }

  private static TimeRange window(PracticeSettings practice, LocalDate from, LocalDate to) {
    return new TimeRange(ZonedDateTime.of(from.atStartOfDay(), practice.zone()).toInstant(),
      ZonedDateTime.of(to.plusDays(1).atStartOfDay(), practice.zone()).toInstant());
  }
}
