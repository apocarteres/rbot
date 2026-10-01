package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.schedule.SessionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// MVP-05, RBOT-FEAT-005, REQ-CODE-DESIGN-001
public record CabinetSession(UUID id, SessionType type, Instant start, Instant end, SessionStatus status, BigDecimal price,
  String clientEmail, boolean cancelledByClient, boolean rescheduled) {
}
