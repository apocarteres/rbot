package com.yanapaderina.rbot.schedule.internal.data;

import java.time.LocalTime;

// MVP-02
public record IntervalRow(int weekday, LocalTime starts, LocalTime ends) {
}
