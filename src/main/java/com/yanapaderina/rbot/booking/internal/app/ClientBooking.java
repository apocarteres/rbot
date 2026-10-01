package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.booking.internal.data.SessionDao;
import com.yanapaderina.rbot.booking.internal.data.SessionRow;
import com.yanapaderina.rbot.schedule.Availability;
import com.yanapaderina.rbot.schedule.BookingTerms;
import com.yanapaderina.rbot.schedule.SessionType;
import com.yanapaderina.rbot.schedule.TimeRange;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, ADR-0003, REQ-DATA-ACCESS-003, REQ-CODE-DESIGN-004
@Service
public class ClientBooking {

  private final Availability availability;
  private final SessionDao sessions;
  private final SessionLedger ledger;
  private final Clock clock;

  ClientBooking(Availability availability, SessionDao sessions, SessionLedger ledger, Clock clock) {
    this.availability = availability;
    this.sessions = sessions;
    this.ledger = ledger;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Optional<ClientOffer> offer() {
    return availability.terms().map(terms -> new ClientOffer(terms, availability.types().stream().filter(SessionType::active).toList()));
  }

  @Transactional(readOnly = true)
  public List<TimeRange> free(UUID typeId, LocalDate from, LocalDate to) {
    open();
    offered(typeId);
    return availability.free(typeId, from, to);
  }

  @Transactional(readOnly = true)
  public List<ClientSession> upcoming(UUID client) {
    Map<UUID, SessionType> types = types();
    return sessions.upcoming(client, clock.instant()).stream().map(row -> session(row, types)).toList();
  }

  @Transactional
  public ClientSession book(UUID client, UUID typeId, Instant start) {
    BookingTerms terms = open();
    SessionType type = offered(typeId);
    requireFree(terms, typeId, start);
    return session(ledger.book(client, type, start, terms.buffer(), client), Map.of(typeId, type));
  }

  @Transactional
  public ClientSession reschedule(UUID client, UUID sessionId, Instant start) {
    BookingTerms terms = open();
    SessionRow row = changeable(client, sessionId, terms.lead());
    SessionType type = offered(row.type());
    requireFree(terms, type.id(), start);
    return session(ledger.reschedule(row, type, start, terms.buffer(), client), Map.of(type.id(), type));
  }

  @Transactional
  public ClientSession cancel(UUID client, UUID sessionId) {
    SessionRow row = changeable(client, sessionId, availability.terms().map(BookingTerms::lead).orElse(Duration.ZERO));
    return session(ledger.close(row, SessionStatus.CANCELLED, client), types());
  }

  private SessionRow changeable(UUID client, UUID sessionId, Duration lead) {
    SessionRow row = sessions.find(sessionId, client).orElseThrow(ClientBooking::missing);
    if (!SessionStatus.BOOKED.name().equals(row.status())) {
      throw new BookingRefused(BookingRefused.SESSION_INACTIVE, "Сессия не назначена");
    }
    if (!CancelDeadline.allows(row.start(), clock.instant(), lead)) {
      throw new BookingRefused(BookingRefused.TOO_LATE, "До начала меньше " + lead.toMinutes() + " мин");
    }
    return row;
  }

  private void requireFree(BookingTerms terms, UUID typeId, Instant start) {
    LocalDate day = start.atZone(terms.zone()).toLocalDate();
    if (availability.free(typeId, day, day).stream().noneMatch(slot -> slot.start().equals(start))) {
      throw new BookingRefused(BookingRefused.SLOT_TAKEN, "Время " + start + " не свободно");
    }
  }

  private BookingTerms open() {
    return availability.terms().orElseThrow(() -> new BookingRefused(BookingRefused.CLOSED, "Параметры записи не заданы"));
  }

  private SessionType offered(UUID typeId) {
    return availability.types().stream().filter(type -> type.id().equals(typeId) && type.active()).findFirst()
      .orElseThrow(() -> new BookingRefused(BookingRefused.TYPE_UNAVAILABLE, "Тип сессии недоступен для записи"));
  }

  private Map<UUID, SessionType> types() {
    return availability.types().stream().collect(Collectors.toMap(SessionType::id, Function.identity()));
  }

  private static ClientSession session(SessionRow row, Map<UUID, SessionType> types) {
    return new ClientSession(row.id(), types.get(row.type()), row.start(), row.end(), SessionStatus.valueOf(row.status()), row.price());
  }

  private static BookingRefused missing() {
    return new BookingRefused(BookingRefused.SESSION_MISSING, "Сессии нет");
  }
}
