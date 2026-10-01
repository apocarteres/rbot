package com.yanapaderina.rbot.clients;

import java.util.Optional;
import java.util.UUID;

// MVP-03, RBOT-FEAT-002, ADR-0005
public interface Clients {

  Optional<UUID> ofAccount(UUID accountId);

  UUID enrolAccount(UUID accountId);
}
