package com.yanapaderina.rbot.clients;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// MVP-03, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, ADR-0002, ADR-0005
public interface Clients {

  Optional<UUID> ofAccount(UUID accountId);

  UUID enrolAccount(UUID accountId);

  Optional<UUID> accountOf(UUID clientId);

  Optional<UUID> ofTelegram(long telegramUserId);

  Optional<Long> telegramOf(UUID clientId);

  List<ClientCard> cards();

  Optional<ClientCard> card(UUID clientId);

  Invitation invite(String label);

  Invitation reinvite(UUID clientId);

  boolean inviteOpen(String token);

  Linking link(String token, long telegramUserId, int consentVersion);
}
