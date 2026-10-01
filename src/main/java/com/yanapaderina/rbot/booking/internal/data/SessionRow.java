package com.yanapaderina.rbot.booking.internal.data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-005
public record SessionRow(UUID id, UUID client, UUID type, Instant start, Instant end, String status, BigDecimal price,
  UUID cancelledBy, UUID rescheduledTo) {

  public SessionRow withStatus(String changed, UUID by) {
    return new SessionRow(id, client, type, start, end, changed, price, by, rescheduledTo);
  }
}
