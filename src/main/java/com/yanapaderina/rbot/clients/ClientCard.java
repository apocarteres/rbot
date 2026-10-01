package com.yanapaderina.rbot.clients;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// MVP-03, RBOT-FEAT-009, RBOT-FEAT-017, REQ-CODE-DESIGN-001
public record ClientCard(UUID id, UUID practitioner, Optional<String> label, Optional<UUID> accountId, boolean telegram, String status,
  Optional<Instant> inviteExpiresAt) {
}
