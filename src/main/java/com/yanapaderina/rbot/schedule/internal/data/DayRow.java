package com.yanapaderina.rbot.schedule.internal.data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// MVP-02, RBOT-FEAT-004
public record DayRow(LocalDate day, boolean closed, String note, LocalTime starts, LocalTime ends, List<UUID> types) {
}
