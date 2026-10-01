package com.yanapaderina.rbot.schedule.internal.data;

// MVP-02
public record SettingsRow(String zone, Integer leadMinutes, Integer horizonDays, Integer slotStepMinutes, Integer bufferMinutes) {
}
