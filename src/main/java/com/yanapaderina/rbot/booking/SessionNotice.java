package com.yanapaderina.rbot.booking;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// MVP-04, MVP-05, RBOT-FEAT-009, ADR-0001
public record SessionNotice(UUID client, Change change, Instant start, Instant end, Optional<Instant> previousStart, String title) {

  public enum Change {
    BOOKED,
    RESCHEDULED,
    CANCELLED
  }
}
