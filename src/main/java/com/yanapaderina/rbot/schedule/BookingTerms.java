package com.yanapaderina.rbot.schedule;

import java.time.Duration;
import java.time.ZoneId;

// MVP-02, MVP-05, RBOT-FEAT-002, RBOT-FEAT-016, ADR-0003
public record BookingTerms(ZoneId zone, Duration lead, int horizonDays) {
}
