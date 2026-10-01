package com.yanapaderina.rbot.booking.internal.data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// MVP-05, RBOT-FEAT-002
public record SessionRow(UUID id, UUID client, UUID type, Instant start, Instant end, String status, BigDecimal price) {
}
