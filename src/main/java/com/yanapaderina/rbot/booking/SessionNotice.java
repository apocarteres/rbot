package com.yanapaderina.rbot.booking;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// MVP-04, MVP-05, RBOT-FEAT-009, RBOT-FEAT-017, RBOT-FEAT-020, ADR-0001
public record SessionNotice(UUID practitioner, UUID client, Change change, Instant start, Instant end, Optional<Instant> previousStart, String title,
  Actor actor) {

  public enum Change {
    BOOKED,
    RESCHEDULED,
    CANCELLED
  }

  public enum Actor {
    PSYCHOLOGIST,
    CLIENT
  }
}
