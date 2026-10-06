package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.booking.internal.data.SessionDao;
import com.yanapaderina.rbot.booking.internal.data.SessionRow;
import com.yanapaderina.rbot.schedule.SessionType;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-016, RBOT-FEAT-017, RBOT-OPS-020, ADR-0003, REQ-CODE-DESIGN-004
@Component
class SessionLedger {

  private final SessionDao sessions;
  private final SessionMeters meters;
  private final Clock clock;

  SessionLedger(SessionDao sessions, SessionMeters meters, Clock clock) {
    this.sessions = sessions;
    this.meters = meters;
    this.clock = clock;
  }

  SessionRow book(UUID practitioner, UUID client, SessionType type, Instant start, UUID by) {
    Instant end = start.plus(type.duration());
    SessionRow row = new SessionRow(UUID.randomUUID(), practitioner, client, type.id(), start, end, SessionStatus.BOOKED.name(),
      type.price(), null, null);
    if (!sessions.insert(row, end.plus(type.buffer()), by, clock.instant())) {
      throw new BookingRefused(BookingRefused.SLOT_TAKEN, "Время " + start + " занято");
    }
    meters.changed("booked", client.equals(by));
    return row;
  }

  SessionRow reschedule(SessionRow old, SessionType type, Instant start, UUID by) {
    closed(old, SessionStatus.CANCELLED, by);
    Instant end = start.plus(type.duration());
    SessionRow moved = new SessionRow(UUID.randomUUID(), old.practitioner(), old.client(), old.type(), start, end,
      SessionStatus.BOOKED.name(), old.price(), null, null);
    if (!sessions.insert(moved, end.plus(type.buffer()), by, clock.instant())) {
      throw new BookingRefused(BookingRefused.SLOT_TAKEN, "Время " + start + " занято");
    }
    sessions.linkReschedule(old.id(), moved.id());
    meters.changed("rescheduled", old.client().equals(by));
    return moved;
  }

  SessionRow close(SessionRow row, SessionStatus status, UUID by) {
    SessionRow closed = closed(row, status, by);
    meters.changed("cancelled", row.client().equals(by));
    return closed;
  }

  private SessionRow closed(SessionRow row, SessionStatus status, UUID by) {
    if (!sessions.close(row.id(), status.name(), by, clock.instant())) {
      throw new BookingRefused(BookingRefused.SESSION_INACTIVE, "Сессия не назначена");
    }
    return row.withStatus(status.name(), by);
  }
}
