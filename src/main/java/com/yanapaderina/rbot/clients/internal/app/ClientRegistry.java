package com.yanapaderina.rbot.clients.internal.app;

import com.yanapaderina.rbot.clients.ClientCard;
import com.yanapaderina.rbot.clients.ClientRefused;
import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.clients.Invitation;
import com.yanapaderina.rbot.clients.Linking;
import com.yanapaderina.rbot.clients.internal.data.ClientDao;
import com.yanapaderina.rbot.clients.internal.data.InviteDao;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-03, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, ADR-0002, ADR-0005, REQ-DATA-ACCESS-003
@Service
class ClientRegistry implements Clients {

  static final Duration INVITE_LIFETIME = Duration.ofDays(30);

  private final ClientDao clients;
  private final InviteDao invites;
  private final Clock clock;

  ClientRegistry(ClientDao clients, InviteDao invites, Clock clock) {
    this.clients = clients;
    this.invites = invites;
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

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> accountOf(UUID clientId) {
    return clients.accountOf(clientId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> ofTelegram(long telegramUserId) {
    return clients.findByTelegram(telegramUserId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Long> telegramOf(UUID clientId) {
    return clients.telegramOf(clientId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientCard> cards() {
    return clients.list();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ClientCard> card(UUID clientId) {
    return clients.find(clientId);
  }

  @Override
  @Transactional
  public Invitation invite(String label) {
    String trimmed = label == null ? "" : label.trim();
    if (trimmed.isEmpty() || trimmed.length() > 100) {
      throw new ClientRefused(ClientRefused.LABEL, "Подпись клиента — от 1 до 100 знаков");
    }
    UUID id = UUID.randomUUID();
    clients.insertProspect(id, trimmed, clock.instant());
    return issue(id);
  }

  @Override
  @Transactional
  public Invitation reinvite(UUID clientId) {
    ClientCard card = clients.find(clientId).orElseThrow(() -> new ClientRefused(ClientRefused.MISSING, "Клиента нет"));
    if (card.accountId().isPresent()) {
      throw new ClientRefused(ClientRefused.NOT_INVITABLE, "Клиент входит по почте");
    }
    invites.withdraw(clientId);
    return issue(clientId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean inviteOpen(String token) {
    return invites.open(InviteTokens.hash(token), clock.instant()).isPresent();
  }

  @Override
  @Transactional
  public Linking link(String token, long telegramUserId, int consentVersion) {
    Instant now = clock.instant();
    String hash = InviteTokens.hash(token);
    Optional<UUID> invited = invites.open(hash, now);
    if (invited.isEmpty()) {
      return new Linking.InviteRejected();
    }
    Optional<UUID> holder = clients.findByTelegram(telegramUserId);
    if (holder.isPresent() && !holder.get().equals(invited.get())) {
      return new Linking.TelegramTaken();
    }
    Optional<UUID> redeemed = invites.redeem(hash, now);
    if (redeemed.isEmpty()) {
      return new Linking.InviteRejected();
    }
    clients.linkTelegram(redeemed.get(), telegramUserId);
    clients.insertConsent(UUID.randomUUID(), redeemed.get(), consentVersion, "TELEGRAM", now);
    return new Linking.Linked(redeemed.get());
  }

  private Invitation issue(UUID clientId) {
    String token = InviteTokens.issue();
    Instant now = clock.instant();
    Instant expires = now.plus(INVITE_LIFETIME);
    invites.insert(UUID.randomUUID(), clientId, InviteTokens.hash(token), expires, now);
    return new Invitation(clientId, token, expires);
  }
}
