package com.yanapaderina.rbot.booking.internal.web;

import com.yanapaderina.rbot.booking.internal.app.BookingRefused;
import com.yanapaderina.rbot.clients.ClientCard;
import com.yanapaderina.rbot.clients.ClientRefused;
import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.clients.ConsentRequest;
import io.github.apocarteres.platform.auth.CurrentIdentity;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// MVP-03, MVP-08, RBOT-FEAT-009, RBOT-FEAT-017, RBOT-FEAT-018, ADR-0002, ADR-0005, ADR-0003, REQ-AUTH-039
@RestController
@RequestMapping("/api/miniapp")
class MiniAppController {

  private final ClientEndpoints endpoints;
  private final Clients clients;

  MiniAppController(ClientEndpoints endpoints, Clients clients) {
    this.endpoints = endpoints;
    this.clients = clients;
  }

  @PostMapping("/invitation")
  ClientViews.Consent invitation(@Valid @RequestBody ClientViews.InviteRequest request) {
    user();
    ConsentRequest invitation = clients.invitation(request.token())
      .orElseThrow(() -> new ClientRefused(ClientRefused.INVITE_REJECTED, "Приглашение не действует"));
    return endpoints.consents(List.of(invitation)).getFirst();
  }

  @PostMapping("/invitation/accept")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void acceptInvitation(@Valid @RequestBody ClientViews.AcceptInvite request) {
    clients.link(request.token(), user(), request.version());
  }

  @GetMapping("/consents")
  List<ClientViews.Consent> consents() {
    return endpoints.consents(clients.consentsDue(user()));
  }

  @PostMapping("/consents")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void consent(@Valid @RequestBody ClientViews.AcceptConsent request) {
    clients.consent(user(), request.practice(), request.version());
  }

  @GetMapping("/practices")
  List<ClientViews.Practice> practices() {
    return endpoints.practices(scope());
  }

  @GetMapping("/offer")
  ClientViews.Offer offer(@RequestParam UUID practice) {
    return endpoints.offer(scope(), practice);
  }

  @GetMapping("/slots")
  List<ClientViews.Slot> slots(@RequestParam UUID practice, @RequestParam UUID type,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return endpoints.slots(scope(), practice, type, from, to);
  }

  @GetMapping("/sessions")
  List<ClientViews.Session> sessions() {
    return endpoints.sessions(scope());
  }

  @PostMapping("/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  ClientViews.Session book(@Valid @RequestBody ClientViews.BookRequest request) {
    return endpoints.book(scope(), request);
  }

  @PostMapping("/sessions/{id}/reschedule")
  ClientViews.Session reschedule(@PathVariable UUID id, @Valid @RequestBody ClientViews.MoveRequest request) {
    return endpoints.reschedule(scope(), id, request);
  }

  @PostMapping("/sessions/{id}/cancel")
  ClientViews.Session cancel(@PathVariable UUID id) {
    return endpoints.cancel(scope(), id);
  }

  private static long user() {
    return CurrentIdentity.get().map(identity -> Long.parseLong(identity.id()))
      .orElseThrow(() -> new BookingRefused(BookingRefused.NOT_LINKED, "Telegram не привязан к клиенту"));
  }

  private ClientScope scope() {
    long user = user();
    Set<UUID> due = clients.consentsDue(user).stream().map(ConsentRequest::practitioner).collect(Collectors.toSet());
    List<ClientCard> cards = clients.byTelegram(user).stream().filter(card -> !due.contains(card.practitioner())).toList();
    if (cards.isEmpty()) {
      throw new BookingRefused(BookingRefused.NOT_LINKED, "Telegram не привязан к клиенту");
    }
    return new ClientScope() {
      @Override
      public List<UUID> practices() {
        return cards.stream().map(ClientCard::practitioner).distinct().toList();
      }

      @Override
      public UUID client(UUID practice) {
        return cards.stream().filter(card -> card.practitioner().equals(practice)).map(ClientCard::id).findFirst()
          .orElseThrow(() -> new BookingRefused(BookingRefused.PRACTICE_MISSING, "Психолог недоступен"));
      }

      @Override
      public Set<UUID> clients() {
        return cards.stream().map(ClientCard::id).collect(Collectors.toSet());
      }
    };
  }
}
