package com.yanapaderina.rbot.clients;

import java.time.Instant;
import java.util.UUID;

// MVP-03, RBOT-FEAT-009, ADR-0002
public record Invitation(UUID clientId, String token, Instant expiresAt) {
}
