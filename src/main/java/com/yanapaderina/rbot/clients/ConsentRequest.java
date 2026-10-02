package com.yanapaderina.rbot.clients;

import java.util.UUID;

// MVP-03, RBOT-FEAT-018, ADR-0005, REQ-CODE-DESIGN-001
public record ConsentRequest(UUID practitioner, ConsentText text) {
}
