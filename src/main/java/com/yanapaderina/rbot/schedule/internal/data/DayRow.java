package com.yanapaderina.rbot.schedule.internal.data;

import java.time.LocalDate;
import java.time.LocalTime;

// MVP-02
public record DayRow(LocalDate day, boolean closed, String note, LocalTime starts, LocalTime ends) {
}
