package com.yanapaderina.rbot.booking.internal.app;

import java.time.Duration;
import java.time.Instant;

// MVP-05, MVP-06, RBOT-FEAT-002, ADR-0004, REQ-CODE-DESIGN-003
final class CancelDeadline {

  private CancelDeadline() {
  }

  static boolean allows(Instant start, Instant now, Duration lead) {
    return !now.plus(lead).isAfter(start);
  }
}
