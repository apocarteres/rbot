package com.yanapaderina.rbot.clients.internal.app;

import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.clients.internal.data.ClientDao;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-03, RBOT-FEAT-002, ADR-0005, REQ-DATA-ACCESS-003
@Service
class ClientRegistry implements Clients {

  private final ClientDao clients;
  private final Clock clock;

  ClientRegistry(ClientDao clients, Clock clock) {
    this.clients = clients;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> ofAccount(UUID accountId) {
    return clients.findByAccount(accountId);
  }

  @Override
  @Transactional
  public UUID enrolAccount(UUID accountId) {
    clients.insertForAccount(UUID.randomUUID(), accountId, clock.instant());
    return clients.findByAccount(accountId).orElseThrow();
  }
}
