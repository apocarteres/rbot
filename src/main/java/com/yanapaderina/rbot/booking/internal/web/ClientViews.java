package com.yanapaderina.rbot.booking.internal.web;

import com.yanapaderina.rbot.booking.internal.app.ClientOffer;
import com.yanapaderina.rbot.booking.internal.app.ClientSession;
import com.yanapaderina.rbot.booking.internal.app.SessionStatus;
import com.yanapaderina.rbot.schedule.SessionFormat;
import com.yanapaderina.rbot.schedule.SessionType;
import com.yanapaderina.rbot.schedule.TimeRange;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// MVP-05, MVP-08, RBOT-FEAT-002, REQ-CODE-DESIGN-005
final class ClientViews {

  private ClientViews() {
  }

  record Type(UUID id, String title, int durationMinutes, BigDecimal price, SessionFormat format, boolean firstVisit) {

    static Type of(SessionType type) {
      return new Type(type.id(), type.title(), Math.toIntExact(type.duration().toMinutes()), type.price(), type.format(),
        type.firstVisit());
    }
  }

  record Offer(boolean open, String zone, Integer leadMinutes, Integer horizonDays, List<Type> types) {

    static Offer of(Optional<ClientOffer> offer) {
      return offer.map(one -> new Offer(true, one.terms().zone().getId(), Math.toIntExact(one.terms().lead().toMinutes()),
          one.terms().horizonDays(), one.types().stream().map(Type::of).toList()))
        .orElseGet(() -> new Offer(false, null, null, null, List.of()));
    }
  }

  record Slot(Instant start, Instant end) {

    static Slot of(TimeRange range) {
      return new Slot(range.start(), range.end());
    }
  }

  record Session(UUID id, UUID typeId, String title, SessionFormat format, Instant start, Instant end, SessionStatus status,
    BigDecimal price) {

    static Session of(ClientSession session) {
      SessionType type = session.type();
      return new Session(session.id(), type == null ? null : type.id(), type == null ? null : type.title(),
        type == null ? null : type.format(), session.start(), session.end(), session.status(), session.price());
    }
  }

  record BookRequest(@NotNull UUID typeId, @NotNull Instant start) {
  }
}
