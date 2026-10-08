package com.yanapaderina.rbot.booking.internal.web;

import com.yanapaderina.rbot.access.Roles;
import com.yanapaderina.rbot.clients.ClientCard;
import com.yanapaderina.rbot.clients.Clients;
import io.github.apocarteres.platform.auth.Accounts;
import io.github.apocarteres.platform.auth.CurrentAccount;
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

// MVP-05, MVP-08, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-017, RBOT-FEAT-026, ADR-0003
@RestController
@RequestMapping("/api/client")
class ClientBookingController {

  private final ClientEndpoints endpoints;
  private final Clients clients;
  private final Accounts accounts;

  ClientBookingController(ClientEndpoints endpoints, Clients clients, Accounts accounts) {
    this.endpoints = endpoints;
    this.clients = clients;
    this.accounts = accounts;
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

  @GetMapping("/sessions/{id}/cancellation")
  ClientViews.Cancellation cancellation(@PathVariable UUID id) {
    return endpoints.cancellation(scope(), id);
  }

  @PostMapping("/sessions/{id}/cancel")
  ClientViews.Session cancel(@PathVariable UUID id) {
    return endpoints.cancel(scope(), id);
  }

  private ClientScope scope() {
    UUID account = CurrentAccount.id().orElseThrow();
    return new ClientScope() {
      @Override
      public List<UUID> practices() {
        return accounts.withRole(Roles.PSYCHOLOGIST);
      }

      @Override
      public UUID client(UUID practice) {
        return clients.enrolAccount(practice, account);
      }

      @Override
      public Set<UUID> clients() {
        return clients.byAccount(account).stream().map(ClientCard::id).collect(Collectors.toSet());
      }
    };
  }
}
