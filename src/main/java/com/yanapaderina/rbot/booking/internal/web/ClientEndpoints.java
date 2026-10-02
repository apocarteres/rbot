package com.yanapaderina.rbot.booking.internal.web;

import com.yanapaderina.rbot.booking.internal.app.BookingRefused;
import com.yanapaderina.rbot.booking.internal.app.ClientBooking;
import com.yanapaderina.rbot.booking.internal.app.ClientSession;
import com.yanapaderina.rbot.clients.ConsentRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

// MVP-05, MVP-08, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-017, RBOT-FEAT-018, ADR-0003
@Component
class ClientEndpoints {

  static final String NAMELESS = "Психолог";

  private final ClientBooking booking;

  ClientEndpoints(ClientBooking booking) {
    this.booking = booking;
  }

  List<ClientViews.Practice> practices(ClientScope scope) {
    List<UUID> practices = scope.practices();
    Map<UUID, String> names = booking.names(practices);
    return practices.stream().map(id -> new ClientViews.Practice(id, name(names, id))).toList();
  }

  ClientViews.Offer offer(ClientScope scope, UUID practice) {
    return ClientViews.Offer.of(booking.offer(allowed(scope, practice)));
  }

  List<ClientViews.Slot> slots(ClientScope scope, UUID practice, UUID type, LocalDate from, LocalDate to) {
    return booking.free(allowed(scope, practice), type, from, to).stream().map(ClientViews.Slot::of).toList();
  }

  List<ClientViews.Session> sessions(ClientScope scope) {
    List<ClientSession> upcoming = booking.upcoming(scope.clients());
    Map<UUID, String> names = booking.names(upcoming.stream().map(ClientSession::practitioner).distinct().toList());
    return upcoming.stream().map(one -> ClientViews.Session.of(one, name(names, one.practitioner()))).toList();
  }

  ClientViews.Session book(ClientScope scope, ClientViews.BookRequest request) {
    UUID practice = allowed(scope, request.practice());
    return named(booking.book(practice, scope.client(practice), request.typeId(), request.start()));
  }

  ClientViews.Session reschedule(ClientScope scope, UUID id, ClientViews.MoveRequest request) {
    return named(booking.reschedule(scope.clients(), id, request.start()));
  }

  ClientViews.Session cancel(ClientScope scope, UUID id) {
    return named(booking.cancel(scope.clients(), id));
  }

  List<ClientViews.Consent> consents(List<ConsentRequest> requests) {
    Map<UUID, String> names = booking.names(requests.stream().map(ConsentRequest::practitioner).distinct().toList());
    return requests.stream().map(one -> new ClientViews.Consent(one.practitioner(), name(names, one.practitioner()), one.text().version(),
      one.text().body())).toList();
  }

  private ClientViews.Session named(ClientSession session) {
    return ClientViews.Session.of(session, name(booking.names(List.of(session.practitioner())), session.practitioner()));
  }

  private static UUID allowed(ClientScope scope, UUID practice) {
    if (practice == null || !scope.practices().contains(practice)) {
      throw new BookingRefused(BookingRefused.PRACTICE_MISSING, "Психолог недоступен");
    }
    return practice;
  }

  private static String name(Map<UUID, String> names, UUID practice) {
    String name = names.get(practice);
    return name == null || name.isBlank() ? NAMELESS : name;
  }
}
