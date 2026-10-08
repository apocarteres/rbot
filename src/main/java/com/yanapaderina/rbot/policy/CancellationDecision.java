package com.yanapaderina.rbot.policy;

// RBOT-FEAT-026, ADR-0004
public record CancellationDecision(boolean allowed, String text) {
}
