package com.yanapaderina.rbot.schedule.internal.data;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// MVP-02, RBOT-FEAT-004
public record IntervalRow(int weekday, LocalTime starts, LocalTime ends, List<UUID> types) {
}
