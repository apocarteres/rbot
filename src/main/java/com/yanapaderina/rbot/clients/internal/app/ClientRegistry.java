package com.yanapaderina.rbot.clients.internal.app;

import com.yanapaderina.rbot.clients.ClientCard;
import com.yanapaderina.rbot.clients.ClientRefused;
import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.clients.ConsentRequest;
import com.yanapaderina.rbot.clients.ConsentText;
import com.yanapaderina.rbot.clients.Invitation;
import com.yanapaderina.rbot.clients.internal.data.ClientDao;
import com.yanapaderina.rbot.clients.internal.data.ConsentDao;
import com.yanapaderina.rbot.clients.internal.data.InviteDao;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// MVP-03, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-017, RBOT-FEAT-018, ADR-0002, ADR-0005, REQ-DATA-ACCESS-003
@Service
class ClientRegistry implements Clients {

  static final Duration INVITE_LIFETIME = Duration.ofDays(30);
  static final int CONSENT_TEXT_LIMIT = 10_000;
  private static final String CHANNEL = "TELEGRAM";

  private final ClientDao clients;
  private final InviteDao invites;
  private final ConsentDao consents;
  private final Clock clock;
  private final ConsentText draft;

  ClientRegistry(ClientDao clients, InviteDao invites, ConsentDao consents, Clock clock) {
    this.clients = clients;
    this.invites = invites;
    this.consents = consents;
    this.clock = clock;
    this.draft = new ConsentText(1, draft(), Optional.empty());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> ofAccount(UUID practitioner, UUID accountId) {
    return clients.findByAccount(practitioner, accountId);
  }

  @Override
  @Transactional
  public UUID enrolAccount(UUID practitioner, UUID accountId) {
    clients.insertForAccount(UUID.randomUUID(), practitioner, accountId, clock.instant());
    return clients.findByAccount(practitioner, accountId).orElseThrow();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientCard> byAccount(UUID accountId) {
    return clients.byAccount(accountId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> accountOf(UUID clientId) {
    return clients.accountOf(clientId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientCard> byTelegram(long telegramUserId) {
    return clients.byTelegram(telegramUserId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Long> telegramOf(UUID clientId) {
    return clients.telegramOf(clientId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientCard> cards(UUID practitioner) {
    return clients.list(practitioner);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ClientCard> card(UUID clientId) {
    return clients.find(clientId);
  }

  @Override
  @Transactional
  public Invitation invite(UUID practitioner, String label) {
    String trimmed = label == null ? "" : label.trim();
    if (trimmed.isEmpty() || trimmed.length() > 100) {
      throw new ClientRefused(ClientRefused.LABEL, "Подпись клиента — от 1 до 100 знаков");
    }
    UUID id = UUID.randomUUID();
    clients.insertProspect(id, practitioner, trimmed, clock.instant());
    return issue(id);
  }

  @Override
  @Transactional
  public Invitation reinvite(UUID practitioner, UUID clientId) {
    ClientCard card = clients.find(clientId).filter(one -> one.practitioner().equals(practitioner))
      .orElseThrow(() -> new ClientRefused(ClientRefused.MISSING, "Клиента нет"));
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
  @Transactional(readOnly = true)
  public Optional<ConsentRequest> invitation(String token) {
    return invites.open(InviteTokens.hash(token), clock.instant()).flatMap(clients::find)
      .map(card -> new ConsentRequest(card.practitioner(), consentText(card.practitioner())));
  }

  @Override
  @Transactional
  public UUID link(String token, long telegramUserId, int consentVersion) {
    Instant now = clock.instant();
    String hash = InviteTokens.hash(token);
    UUID invited = invites.open(hash, now).orElseThrow(ClientRegistry::inviteRejected);
    UUID practitioner = clients.find(invited).orElseThrow().practitioner();
    Optional<UUID> holder = clients.findByTelegram(practitioner, telegramUserId);
    if (holder.isPresent() && !holder.get().equals(invited)) {
      throw new ClientRefused(ClientRefused.TELEGRAM_TAKEN, "Telegram уже привязан к другому клиенту");
    }
    current(practitioner, consentVersion);
    UUID redeemed = invites.redeem(hash, now).orElseThrow(ClientRegistry::inviteRejected);
    clients.linkTelegram(redeemed, telegramUserId);
    consents.insert(UUID.randomUUID(), redeemed, consentVersion, CHANNEL, now);
    return redeemed;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ConsentRequest> consentsDue(long telegramUserId) {
    return consents.due(telegramUserId).stream().map(practitioner -> new ConsentRequest(practitioner, consentText(practitioner))).toList();
  }

  @Override
  @Transactional
  public void consent(long telegramUserId, UUID practitioner, int consentVersion) {
    UUID client = clients.findByTelegram(practitioner, telegramUserId)
      .orElseThrow(() -> new ClientRefused(ClientRefused.MISSING, "Клиента нет"));
    current(practitioner, consentVersion);
    consents.insert(UUID.randomUUID(), client, consentVersion, CHANNEL, clock.instant());
  }

  @Override
  @Transactional(readOnly = true)
  public ConsentText consentText(UUID practitioner) {
    return consents.latestText(practitioner).orElse(draft);
  }

  @Override
  @Transactional
  public ConsentText saveConsentText(UUID practitioner, String body) {
    String text = body == null ? "" : body.replace("\r\n", "\n").strip();
    if (text.isEmpty() || text.length() > CONSENT_TEXT_LIMIT) {
      throw new ClientRefused(ClientRefused.CONSENT_TEXT, "Текст согласия — от 1 до " + CONSENT_TEXT_LIMIT + " знаков");
    }
    Optional<ConsentText> latest = consents.latestText(practitioner);
    if (latest.isPresent() && latest.get().body().equals(text)) {
      return latest.get();
    }
    int version = latest.map(one -> one.version() + 1).orElse(1);
    if (!consents.insertText(UUID.randomUUID(), practitioner, version, text, clock.instant())) {
      throw new ClientRefused(ClientRefused.CONSENT_TEXT_CHANGED, "Текст согласия только что изменили");
    }
    return consents.latestText(practitioner).orElseThrow();
  }

  private void current(UUID practitioner, int consentVersion) {
    if (consents.latestText(practitioner).isEmpty()) {
      consents.insertText(UUID.randomUUID(), practitioner, draft.version(), draft.body(), clock.instant());
    }
    if (consents.latestText(practitioner).orElseThrow().version() != consentVersion) {
      throw new ClientRefused(ClientRefused.CONSENT_OUTDATED, "Текст согласия обновился");
    }
  }

  private static ClientRefused inviteRejected() {
    return new ClientRefused(ClientRefused.INVITE_REJECTED, "Приглашение не действует");
  }

  private static String draft() {
    try {
      return new ClassPathResource("clients/consent-draft.txt").getContentAsString(StandardCharsets.UTF_8).strip();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Invitation issue(UUID clientId) {
    String token = InviteTokens.issue();
    Instant now = clock.instant();
    Instant expires = now.plus(INVITE_LIFETIME);
    invites.insert(UUID.randomUUID(), clientId, InviteTokens.hash(token), expires, now);
    return new Invitation(clientId, token, expires);
  }
}
