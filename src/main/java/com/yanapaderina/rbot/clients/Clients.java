package com.yanapaderina.rbot.clients;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// MVP-03, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-017, RBOT-FEAT-018, ADR-0002, ADR-0005
public interface Clients {

  Optional<UUID> ofAccount(UUID practitioner, UUID accountId);

  UUID enrolAccount(UUID practitioner, UUID accountId);

  List<ClientCard> byAccount(UUID accountId);

  List<ClientCard> byTelegram(long telegramUserId);

  Optional<UUID> accountOf(UUID clientId);

  Optional<Long> telegramOf(UUID clientId);

  List<ClientCard> cards(UUID practitioner);

  Optional<ClientCard> card(UUID clientId);

  Invitation invite(UUID practitioner, String label);

  Invitation reinvite(UUID practitioner, UUID clientId);

  boolean inviteOpen(String token);

  Optional<ConsentRequest> invitation(String token);

  UUID link(String token, long telegramUserId, int consentVersion);

  List<ConsentRequest> consentsDue(long telegramUserId);

  void consent(long telegramUserId, UUID practitioner, int consentVersion);

  ConsentText consentText(UUID practitioner);

  ConsentText saveConsentText(UUID practitioner, String body);
}
