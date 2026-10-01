package com.yanapaderina.rbot.booking.internal.web;

import com.yanapaderina.rbot.booking.internal.app.ClientBooking;
import com.yanapaderina.rbot.clients.Clients;
import io.github.apocarteres.platform.auth.CurrentAccount;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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

// MVP-05, MVP-08, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, ADR-0003
@RestController
@RequestMapping("/api/client")
class ClientBookingController {

  private final ClientBooking booking;
  private final Clients clients;

  ClientBookingController(ClientBooking booking, Clients clients) {
    this.booking = booking;
    this.clients = clients;
  }

  @GetMapping("/offer")
  ClientViews.Offer offer() {
    return ClientViews.Offer.of(booking.offer());
  }

  @GetMapping("/slots")
  List<ClientViews.Slot> slots(@RequestParam UUID type, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return booking.free(type, from, to).stream().map(ClientViews.Slot::of).toList();
  }

  @GetMapping("/sessions")
  List<ClientViews.Session> sessions() {
    return booking.upcoming(account()).stream().map(ClientViews.Session::of).toList();
  }

  @PostMapping("/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  ClientViews.Session book(@Valid @RequestBody ClientViews.BookRequest request) {
    return ClientViews.Session.of(booking.book(account(), request.typeId(), request.start()));
  }

  @PostMapping("/sessions/{id}/reschedule")
  ClientViews.Session reschedule(@PathVariable UUID id, @Valid @RequestBody ClientViews.MoveRequest request) {
    return ClientViews.Session.of(booking.reschedule(account(), id, request.start()));
  }

  @PostMapping("/sessions/{id}/cancel")
  ClientViews.Session cancel(@PathVariable UUID id) {
    return ClientViews.Session.of(booking.cancel(account(), id));
  }

  private UUID account() {
    return clients.enrolAccount(CurrentAccount.id().orElseThrow());
  }
}
