package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.booking.internal.data.SessionDao;
import com.yanapaderina.rbot.booking.internal.data.SessionRow;
import com.yanapaderina.rbot.schedule.SessionType;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-016, ADR-0003, REQ-CODE-DESIGN-004
@Component
class SessionLedger {

  private final SessionDao sessions;
  private final Clock clock;

  SessionLedger(SessionDao sessions, Clock clock) {
    this.sessions = sessions;
    this.clock = clock;
  }

  SessionRow book(UUID client, SessionType type, Instant start, UUID by) {
    Instant end = start.plus(type.duration());
    SessionRow row = new SessionRow(UUID.randomUUID(), client, type.id(), start, end, SessionStatus.BOOKED.name(), type.price(), null,
      null);
    if (!sessions.insert(row, end.plus(type.buffer()), by, clock.instant())) {
      throw new BookingRefused(BookingRefused.SLOT_TAKEN, "Время " + start + " занято");
    }
    return row;
  }

  SessionRow reschedule(SessionRow old, SessionType type, Instant start, UUID by) {
    close(old, SessionStatus.CANCELLED, by);
    Instant end = start.plus(type.duration());
    SessionRow moved = new SessionRow(UUID.randomUUID(), old.client(), old.type(), start, end, SessionStatus.BOOKED.name(),
      old.price(), null, null);
    if (!sessions.insert(moved, end.plus(type.buffer()), by, clock.instant())) {
      throw new BookingRefused(BookingRefused.SLOT_TAKEN, "Время " + start + " занято");
    }
    sessions.linkReschedule(old.id(), moved.id());
    return moved;
  }

  SessionRow close(SessionRow row, SessionStatus status, UUID by) {
    if (!sessions.close(row.id(), status.name(), by, clock.instant())) {
      throw new BookingRefused(BookingRefused.SESSION_INACTIVE, "Сессия не назначена");
    }
    return row.withStatus(status.name(), by);
  }
}
