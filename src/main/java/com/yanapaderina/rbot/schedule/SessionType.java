package com.yanapaderina.rbot.schedule;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

// MVP-02, RBOT-FEAT-002, ADR-0003, REQ-CODE-DESIGN-001
public record SessionType(UUID id, String title, Duration duration, BigDecimal price, SessionFormat format, boolean firstVisit,
  boolean active) {
}
