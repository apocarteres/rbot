package com.yanapaderina.rbot.clients;

import java.util.Optional;
import java.util.UUID;

// RBOT-FEAT-019, RBOT-FEAT-017, REQ-CODE-DESIGN-002
public interface PracticeNames {

  Optional<String> name(UUID practitioner);
}
