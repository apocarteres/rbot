package com.yanapaderina.rbot.booking.internal.data;

import java.time.Instant;

// MVP-05, RBOT-FEAT-002
public record OccupiedRow(Instant start, Instant end) {
}
