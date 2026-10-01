package com.yanapaderina.rbot.schedule.internal.data;

import java.math.BigDecimal;
import java.util.UUID;

// MVP-02, REQ-CODE-DESIGN-001
public record SessionTypeRow(UUID id, String title, int durationMinutes, BigDecimal price, String format, boolean firstVisit,
  boolean active) {
}
