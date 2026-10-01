package com.yanapaderina.rbot.schedule.internal.data;

import java.math.BigDecimal;
import java.util.UUID;

// MVP-02, RBOT-FEAT-016, REQ-CODE-DESIGN-001
public record SessionTypeRow(UUID id, String title, int durationMinutes, BigDecimal price, int bufferMinutes, String format,
  boolean active) {
}
