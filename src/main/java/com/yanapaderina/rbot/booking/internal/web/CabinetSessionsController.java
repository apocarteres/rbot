package com.yanapaderina.rbot.booking.internal.web;

import com.yanapaderina.rbot.booking.internal.app.CabinetBooking;
import com.yanapaderina.rbot.booking.internal.app.CabinetSession;
import com.yanapaderina.rbot.booking.internal.app.SessionStatus;
import com.yanapaderina.rbot.schedule.SessionFormat;
import io.github.apocarteres.platform.auth.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
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

// MVP-05, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-017, ADR-0003, REQ-CODE-DESIGN-005
@RestController
@RequestMapping("/api/cabinet")
class CabinetSessionsController {

  private final CabinetBooking booking;

  CabinetSessionsController(CabinetBooking booking) {
    this.booking = booking;
  }

  @GetMapping("/sessions")
  List<Session> sessions(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return booking.between(me(), from, to).stream().map(Session::of).toList();
  }

  @PostMapping("/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  Session book(@Valid @RequestBody BookRequest request) {
    return Session.of(booking.book(me(), request.clientId(), request.typeId(), request.start()));
  }

  @PostMapping("/sessions/{id}/reschedule")
  Session reschedule(@PathVariable UUID id, @Valid @RequestBody MoveRequest request) {
    return Session.of(booking.reschedule(me(), id, request.start()));
  }

  @PostMapping("/sessions/{id}/cancel")
  Session cancel(@PathVariable UUID id) {
    return Session.of(booking.cancel(me(), id));
  }

  @PostMapping("/sessions/{id}/no-show")
  Session noShow(@PathVariable UUID id) {
    return Session.of(booking.noShow(me(), id));
  }

  private static UUID me() {
    return CurrentAccount.id().orElseThrow();
  }

  record Session(UUID id, UUID typeId, String title, SessionFormat format, Instant start, Instant end, SessionStatus status,
    BigDecimal price, String clientName, boolean cancelledByClient, boolean rescheduled) {

    static Session of(CabinetSession session) {
      return new Session(session.id(), session.type() == null ? null : session.type().id(),
        session.type() == null ? null : session.type().title(), session.type() == null ? null : session.type().format(),
        session.start(), session.end(), session.status(), session.price(), session.clientName(), session.cancelledByClient(),
        session.rescheduled());
    }
  }

  record BookRequest(@NotNull UUID clientId, @NotNull UUID typeId, @NotNull Instant start) {
  }

  record MoveRequest(@NotNull Instant start) {
  }
}
