package com.yanapaderina.rbot.policy;

import java.util.UUID;

// RBOT-FEAT-026, ADR-0004
public record CancellationRule(UUID id, int hours, boolean allowed, String text) {
}
