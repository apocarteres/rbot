package com.yanapaderina.rbot.clients;

import java.time.Instant;
import java.util.Optional;

// MVP-03, RBOT-FEAT-018, ADR-0005, REQ-CODE-DESIGN-001
public record ConsentText(int version, String body, Optional<Instant> savedAt) {
}
