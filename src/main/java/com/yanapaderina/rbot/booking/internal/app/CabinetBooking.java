package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.access.Roles;
import com.yanapaderina.rbot.booking.internal.data.SessionDao;
import com.yanapaderina.rbot.booking.internal.data.SessionRow;
import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.schedule.Availability;
import com.yanapaderina.rbot.schedule.BookingTerms;
import com.yanapaderina.rbot.schedule.SessionType;
import io.github.apocarteres.platform.auth.Account;
import io.github.apocarteres.platform.auth.Accounts;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-05, RBOT-FEAT-005, ADR-0003, REQ-DATA-ACCESS-003, REQ-CODE-DESIGN-004
@Service
public class CabinetBooking {

  static final int MAX_RANGE_DAYS = 62;

  private final Availability availability;
  private final Clients clients;
  private final Accounts accounts;
  private final SessionDao sessions;
  private final SessionLedger ledger;
  private final Clock clock;

  CabinetBooking(Availability availability, Clients clients, Accounts accounts, SessionDao sessions, SessionLedger ledger,
    Clock clock) {
    this.availability = availability;
    this.clients = clients;
    this.accounts = accounts;
    this.sessions = sessions;
    this.ledger = ledger;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<CabinetSession> between(LocalDate from, LocalDate to) {
    if (to.isBefore(from) || from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
      throw new BookingRefused(BookingRefused.RANGE, "Диапазон " + from + "–" + to);
    }
    ZoneId zone = availability.zone();
    Map<UUID, SessionType> types = types();
    Map<UUID, Optional<UUID>> owners = new HashMap<>();
    return sessions.between(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant()).stream()
      .map(row -> {
        Optional<UUID> owner = owners.computeIfAbsent(row.client(), clients::accountOf);
        return new CabinetSession(row.id(), types.get(row.type()), row.start(), row.end(), SessionStatus.valueOf(row.status()),
          row.price(), owner.flatMap(accounts::find).map(Account::email).orElse(null),
          row.cancelledBy() != null && owner.filter(row.cancelledBy()::equals).isPresent(), row.rescheduledTo() != null);
      })
      .toList();
  }

  @Transactional(readOnly = true)
  public List<ClientAccount> clientAccounts() {
    return accounts.withRole(Roles.CLIENT).stream().map(accounts::find).flatMap(Optional::stream)
      .filter(account -> !account.blocked())
      .map(account -> new ClientAccount(account.id(), account.email()))
      .sorted(Comparator.comparing(ClientAccount::email))
      .toList();
  }

  @Transactional
  public CabinetSession book(UUID clientAccountId, UUID typeId, Instant start, UUID by) {
    Account client = accounts.find(clientAccountId).filter(account -> account.has(Roles.CLIENT))
      .orElseThrow(() -> new BookingRefused(BookingRefused.CLIENT_MISSING, "Клиента нет"));
    SessionType type = type(typeId);
    future(start);
    SessionRow row = ledger.book(clients.enrolAccount(client.id()), type, start, buffer(), by);
    return view(row, type, client.email());
  }

  @Transactional
  public CabinetSession reschedule(UUID sessionId, Instant start, UUID by) {
    SessionRow row = booked(sessionId);
    SessionType type = type(row.type());
    future(start);
    return view(ledger.reschedule(row, type, start, buffer(), by), type, email(row.client()));
  }

  @Transactional
  public CabinetSession cancel(UUID sessionId, UUID by) {
    SessionRow row = booked(sessionId);
    return view(ledger.close(row, SessionStatus.CANCELLED, by), types().get(row.type()), email(row.client()));
  }

  @Transactional
  public CabinetSession noShow(UUID sessionId) {
    SessionRow row = booked(sessionId);
    if (!sessions.markNoShow(sessionId, clock.instant())) {
      throw new BookingRefused(BookingRefused.NOT_STARTED, "Сессия ещё не началась");
    }
    return view(row.withStatus(SessionStatus.NO_SHOW.name(), row.cancelledBy()), types().get(row.type()), email(row.client()));
  }

  private SessionRow booked(UUID sessionId) {
    SessionRow row = sessions.find(sessionId).orElseThrow(() -> new BookingRefused(BookingRefused.SESSION_MISSING, "Сессии нет"));
    if (!SessionStatus.BOOKED.name().equals(row.status())) {
      throw new BookingRefused(BookingRefused.SESSION_INACTIVE, "Сессия не назначена");
    }
    return row;
  }

  private void future(Instant start) {
    if (!start.isAfter(clock.instant())) {
      throw new BookingRefused(BookingRefused.START_PAST, "Начало " + start + " в прошлом");
    }
  }

  private SessionType type(UUID typeId) {
    return Optional.ofNullable(types().get(typeId))
      .orElseThrow(() -> new BookingRefused(BookingRefused.TYPE_UNAVAILABLE, "Типа сессии нет"));
  }

  private Duration buffer() {
    return availability.terms().map(BookingTerms::buffer).orElse(Duration.ZERO);
  }

  private String email(UUID client) {
    return clients.accountOf(client).flatMap(accounts::find).map(Account::email).orElse(null);
  }

  private Map<UUID, SessionType> types() {
    return availability.types().stream().collect(Collectors.toMap(SessionType::id, Function.identity()));
  }

  private static CabinetSession view(SessionRow row, SessionType type, String email) {
    return new CabinetSession(row.id(), type, row.start(), row.end(), SessionStatus.valueOf(row.status()), row.price(), email, false,
      row.rescheduledTo() != null);
  }
}
