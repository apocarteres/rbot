package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.booking.SessionNotice;
import com.yanapaderina.rbot.booking.internal.data.SessionDao;
import com.yanapaderina.rbot.booking.internal.data.SessionRow;
import com.yanapaderina.rbot.policy.CancellationDecision;
import com.yanapaderina.rbot.policy.CancellationRule;
import com.yanapaderina.rbot.policy.CancellationRules;
import com.yanapaderina.rbot.schedule.Availability;
import com.yanapaderina.rbot.schedule.BookingTerms;
import com.yanapaderina.rbot.schedule.SessionType;
import com.yanapaderina.rbot.schedule.TimeRange;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-016, RBOT-FEAT-017, ADR-0003, REQ-DATA-ACCESS-003, REQ-CODE-DESIGN-004, RBOT-FEAT-020, RBOT-FEAT-026, ADR-0004
@Service
public class ClientBooking {

  private final Availability availability;
  private final SessionDao sessions;
  private final SessionLedger ledger;
  private final ApplicationEventPublisher events;
  private final CancellationRules rules;
  private final Clock clock;

  ClientBooking(Availability availability, SessionDao sessions, SessionLedger ledger, ApplicationEventPublisher events,
    CancellationRules rules, Clock clock) {
    this.availability = availability;
    this.sessions = sessions;
    this.ledger = ledger;
    this.events = events;
    this.rules = rules;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Map<UUID, String> names(Collection<UUID> practitioners) {
    return availability.names(practitioners);
  }

  @Transactional(readOnly = true)
  public Optional<ClientOffer> offer(UUID practitioner) {
    return availability.terms(practitioner)
      .map(terms -> new ClientOffer(terms, availability.types(practitioner).stream().filter(SessionType::active).toList(),
        rules.rules(practitioner).stream().map(CancellationRule::text).toList()));
  }

  @Transactional(readOnly = true)
  public List<TimeRange> free(UUID practitioner, UUID typeId, LocalDate from, LocalDate to) {
    open(practitioner);
    offered(practitioner, typeId);
    return availability.free(practitioner, typeId, from, to);
  }

  @Transactional(readOnly = true)
  public List<ClientSession> upcoming(Collection<UUID> clients) {
    Map<UUID, Map<UUID, SessionType>> types = new HashMap<>();
    return sessions.upcoming(clients, clock.instant()).stream()
      .map(row -> session(row, types.computeIfAbsent(row.practitioner(), this::everyType)))
      .toList();
  }

  @Transactional
  public ClientSession book(UUID practitioner, UUID client, UUID typeId, Instant start) {
    BookingTerms terms = open(practitioner);
    SessionType type = offered(practitioner, typeId);
    requireFree(practitioner, terms, typeId, start);
    SessionRow row = ledger.book(practitioner, client, type, start, client);
    told(row, SessionNotice.Change.BOOKED, Optional.empty(), type.title());
    return session(row, Map.of(typeId, type));
  }

  @Transactional(readOnly = true)
  public CancellationDecision cancellation(Set<UUID> clients, UUID sessionId) {
    SessionRow row = mine(clients, sessionId);
    active(row);
    return rules.decide(row.practitioner(), row.start());
  }

  @Transactional
  public ClientSession reschedule(Set<UUID> clients, UUID sessionId, Instant start) {
    SessionRow row = mine(clients, sessionId);
    BookingTerms terms = open(row.practitioner());
    changeable(row);
    SessionType type = offered(row.practitioner(), row.type());
    requireFree(row.practitioner(), terms, type.id(), start);
    SessionRow moved = ledger.reschedule(row, type, start, row.client());
    told(moved, SessionNotice.Change.RESCHEDULED, Optional.of(row.start()), type.title());
    return session(moved, Map.of(type.id(), type));
  }

  @Transactional
  public ClientSession cancel(Set<UUID> clients, UUID sessionId) {
    SessionRow row = mine(clients, sessionId);
    changeable(row);
    Map<UUID, SessionType> types = everyType(row.practitioner());
    SessionRow closed = ledger.close(row, SessionStatus.CANCELLED, row.client());
    told(closed, SessionNotice.Change.CANCELLED, Optional.empty(), types.containsKey(row.type()) ? types.get(row.type()).title() : "");
    return session(closed, types);
  }

  private void told(SessionRow row, SessionNotice.Change change, Optional<Instant> previous, String title) {
    events.publishEvent(new SessionNotice(row.practitioner(), row.client(), change, row.start(), row.end(), previous, title,
      SessionNotice.Actor.CLIENT));
  }

  private SessionRow mine(Set<UUID> clients, UUID sessionId) {
    return sessions.find(sessionId).filter(row -> clients.contains(row.client())).orElseThrow(ClientBooking::missing);
  }

  private void changeable(SessionRow row) {
    active(row);
    CancellationDecision decision = rules.decide(row.practitioner(), row.start());
    if (!decision.allowed()) {
      throw new BookingRefused(BookingRefused.CANCEL_FORBIDDEN, decision.text());
    }
  }

  private static void active(SessionRow row) {
    if (!SessionStatus.BOOKED.name().equals(row.status())) {
      throw new BookingRefused(BookingRefused.SESSION_INACTIVE, "Сессия не назначена");
    }
  }

  private void requireFree(UUID practitioner, BookingTerms terms, UUID typeId, Instant start) {
    LocalDate day = start.atZone(terms.zone()).toLocalDate();
    if (availability.free(practitioner, typeId, day, day).stream().noneMatch(slot -> slot.start().equals(start))) {
      throw new BookingRefused(BookingRefused.SLOT_TAKEN, "Время " + start + " не свободно");
    }
  }

  private BookingTerms open(UUID practitioner) {
    return availability.terms(practitioner)
      .orElseThrow(() -> new BookingRefused(BookingRefused.CLOSED, "Параметры записи не заданы"));
  }

  private SessionType offered(UUID practitioner, UUID typeId) {
    return availability.types(practitioner).stream().filter(type -> type.id().equals(typeId) && type.active()).findFirst()
      .orElseThrow(() -> new BookingRefused(BookingRefused.TYPE_UNAVAILABLE, "Тип сессии недоступен для записи"));
  }

  private Map<UUID, SessionType> everyType(UUID practitioner) {
    Map<UUID, SessionType> types = new HashMap<>();
    availability.everyType(practitioner).forEach(type -> types.put(type.id(), type));
    return types;
  }

  private static ClientSession session(SessionRow row, Map<UUID, SessionType> types) {
    return new ClientSession(row.id(), row.practitioner(), types.get(row.type()), row.start(), row.end(),
      SessionStatus.valueOf(row.status()), row.price());
  }

  private static BookingRefused missing() {
    return new BookingRefused(BookingRefused.SESSION_MISSING, "Сессии нет");
  }
}
