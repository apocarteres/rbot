package com.yanapaderina.rbot.clients.internal.web;

import com.yanapaderina.rbot.clients.ClientCard;
import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.clients.Invitation;
import io.github.apocarteres.platform.auth.Account;
import io.github.apocarteres.platform.auth.Accounts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// MVP-03, RBOT-FEAT-009, ADR-0002, REQ-CODE-DESIGN-005
@RestController
@RequestMapping("/api/cabinet/clients")
class CabinetClientsController {

  private static final String CLIENT_ROLE = "CLIENT";

  private final Clients clients;
  private final Accounts accounts;
  private final String botUsername;

  CabinetClientsController(Clients clients, Accounts accounts, @Value("${rbot.telegram.bot-username:}") String botUsername) {
    this.clients = clients;
    this.accounts = accounts;
    this.botUsername = botUsername;
  }

  @GetMapping
  List<ClientView> list() {
    accounts.withRole(CLIENT_ROLE).forEach(clients::enrolAccount);
    return clients.cards().stream().map(this::view).filter(view -> view.name() != null)
      .sorted(Comparator.comparing(ClientView::name, String.CASE_INSENSITIVE_ORDER)).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  InviteView invite(@Valid @RequestBody InviteRequest request) {
    return invite(clients.invite(request.label()));
  }

  @PostMapping("/{id}/invite")
  InviteView reinvite(@PathVariable UUID id) {
    return invite(clients.reinvite(id));
  }

  private InviteView invite(Invitation invitation) {
    String link = botUsername.isBlank() ? null : "https://t.me/" + botUsername + "?start=" + invitation.token();
    return new InviteView(invitation.clientId(), link, invitation.expiresAt());
  }

  private ClientView view(ClientCard card) {
    String email = card.accountId().flatMap(accounts::find).map(Account::email).orElse(null);
    String name = card.label().orElse(email);
    String channel = card.telegram() ? "TELEGRAM" : email != null ? "EMAIL" : "INVITED";
    return new ClientView(card.id(), name, email, channel, card.inviteExpiresAt().orElse(null));
  }

  record ClientView(UUID id, String name, String email, String channel, Instant inviteExpiresAt) {
  }

  record InviteView(UUID clientId, String link, Instant expiresAt) {
  }

  record InviteRequest(@NotNull String label) {
  }
}
