package com.yanapaderina.rbot.schedule;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

// MVP-02, RBOT-FEAT-002, RBOT-FEAT-016, ADR-0003, REQ-CODE-DESIGN-001
public record SessionType(UUID id, String title, Duration duration, Duration buffer, BigDecimal price, SessionFormat format,
  boolean active) {
}
