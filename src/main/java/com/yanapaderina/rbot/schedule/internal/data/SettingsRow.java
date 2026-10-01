package com.yanapaderina.rbot.schedule.internal.data;

// MVP-02, RBOT-FEAT-016
public record SettingsRow(String zone, Integer leadMinutes, Integer horizonDays, Integer slotStepMinutes) {
}
