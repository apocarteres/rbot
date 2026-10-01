package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.schedule.SessionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-017, ADR-0003, REQ-CODE-DESIGN-001
public record ClientSession(UUID id, UUID practitioner, SessionType type, Instant start, Instant end, SessionStatus status,
  BigDecimal price) {
}
