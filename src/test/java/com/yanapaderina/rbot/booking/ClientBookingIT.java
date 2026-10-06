package com.yanapaderina.rbot.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yanapaderina.rbot.IntegrationStores;
import com.yanapaderina.rbot.booking.internal.app.BookingRefused;
import com.yanapaderina.rbot.booking.internal.app.ClientBooking;
import com.yanapaderina.rbot.clients.Clients;
import io.github.apocarteres.platform.auth.Accounts;
import io.github.apocarteres.platform.auth.NoProfile;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-016, RBOT-FEAT-017, RBOT-FEAT-020, RBOT-FEAT-021, ADR-0003
@SpringBootTest(properties = {
  "platform.auth.admin.email=booking-psychologist@example.test",
  "platform.auth.admin.password=psychologist-password-1",
  "platform.auth.admin.roles=PSYCHOLOGIST"
})
@AutoConfigureMockMvc
class ClientBookingIT extends IntegrationStores {

  private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
  private static String therapy;
  private static String offline;
  private static final String PASSWORD = "client-password-1";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private Clock clock;

  @Autowired
  private Accounts accounts;

  @Autowired
  private ClientBooking booking;

  @Autowired
  private Clients clients;

  @BeforeEach
  void practice() throws Exception {
    for (String email : List.of("first@example.test", "second@example.test", "third@example.test", "fourth@example.test")) {
      if (accounts.findByEmail(email).isEmpty()) {
        accounts.create(email, PASSWORD, Set.of("CLIENT"), true, new NoProfile());
      }
    }
    Cookie[] psychologist = login("booking-psychologist@example.test", "psychologist-password-1");
    if (therapy == null) {
      therapy = type(psychologist, "Психотерапия очно", 60, 0, true);
      offline = type(psychologist, "Психотерапия онлайн", 60, 0, false);
    }
    settings(psychologist, 0);
    for (int weekday = 1; weekday <= 7; weekday++) {
      write(put("/api/cabinet/schedule/week/" + weekday), psychologist, "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"13:00\",\"types\":[\"" + therapy + "\"]}]}", 200);
    }
    openAll(psychologist, today().plusDays(1), today().plusDays(7));
  }

  @Test
  void onlyOpenedTimeIsOffered() throws Exception {
    Cookie[] psychologist = login("booking-psychologist@example.test", "psychologist-password-1");
    Cookie[] third = login("third@example.test", PASSWORD);
    LocalDate day = today().plusDays(9);
    String start = at(day, "10:00").toString();
    openings(psychologist, day)
      .andExpect(jsonPath("$.length()").value(3))
      .andExpect(jsonPath("$[0].state").value("CLOSED"));
    slots(third, day).andExpect(jsonPath("$.length()").value(0));
    String request = "{\"practice\":\"" + practiceId() + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + start + "\"}";
    write(post("/api/client/sessions"), third, request, 409).andExpect(jsonPath("$.code").value("slot-taken"));

    write(put("/api/cabinet/schedule/openings"), psychologist, "{\"open\":[\"" + start + "\"],\"close\":[]}", 204);
    openings(psychologist, day).andExpect(jsonPath("$[0].state").value("OPEN")).andExpect(jsonPath("$[1].state").value("CLOSED"));
    slots(third, day).andExpect(jsonPath("$.length()").value(1));
    write(put("/api/cabinet/schedule/openings"), psychologist, "{\"open\":[],\"close\":[\"" + start + "\"]}", 204);
    slots(third, day).andExpect(jsonPath("$.length()").value(0));

    write(put("/api/cabinet/schedule/openings"), psychologist, "{\"open\":[\"" + start + "\"],\"close\":[]}", 204);
    write(post("/api/client/sessions"), third, request, 201);
    openings(psychologist, day).andExpect(jsonPath("$[0].state").value("BUSY"));
    write(put("/api/cabinet/schedule/openings"), psychologist, "{\"open\":[\"2020-01-01T10:00:00Z\"],\"close\":[]}", 400)
      .andExpect(jsonPath("$.code").value("opening-rejected"));
  }

  @Test
  void clientChangesRingPsychologistBell() throws Exception {
    Cookie[] psychologist = login("booking-psychologist@example.test", "psychologist-password-1");
    Cookie[] fourth = login("fourth@example.test", PASSWORD);
    write(post("/api/notifications/read-all"), psychologist, "", 204);
    LocalDate day = today().plusDays(5);
    String start = at(day, "11:00").toString();
    String booked = write(post("/api/client/sessions"), fourth,
      "{\"practice\":\"" + practiceId() + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + start + "\"}", 201)
      .andReturn().getResponse().getContentAsString();
    mvc.perform(get("/api/notifications").cookie(psychologist))
      .andExpect(jsonPath("$.unread").value(1))
      .andExpect(jsonPath("$.items[0].kind").value("session.booked"))
      .andExpect(jsonPath("$.items[0].params.type").value("Психотерапия очно"))
      .andExpect(jsonPath("$.items[0].params.start").value(start))
      .andExpect(jsonPath("$.items[0].link").value("/sessions?at=" + start));
    String moved = at(day, "12:00").toString();
    write(post("/api/client/sessions/" + JsonPath.read(booked, "$.id") + "/reschedule"), fourth, "{\"start\":\"" + moved + "\"}", 200);
    mvc.perform(get("/api/notifications").cookie(psychologist))
      .andExpect(jsonPath("$.unread").value(2))
      .andExpect(jsonPath("$.items[0].kind").value("session.rescheduled"))
      .andExpect(jsonPath("$.items[0].params.previous").value(start));
    String bookedByPsychologist = write(post("/api/cabinet/sessions"), psychologist,
      "{\"clientId\":\"" + clients.ofAccount(java.util.UUID.fromString(practiceId()),
        accounts.findByEmail("fourth@example.test").orElseThrow().id()).orElseThrow() + "\",\"typeId\":\"" + therapy
        + "\",\"start\":\"" + at(day, "10:00") + "\"}", 201).andReturn().getResponse().getContentAsString();
    mvc.perform(get("/api/notifications/unread").cookie(psychologist)).andExpect(jsonPath("$.count").value(2));
    write(post("/api/client/sessions/" + JsonPath.read(bookedByPsychologist, "$.id") + "/cancel"), fourth, "", 200);
    mvc.perform(get("/api/notifications").cookie(psychologist))
      .andExpect(jsonPath("$.unread").value(3))
      .andExpect(jsonPath("$.items[0].kind").value("session.cancelled"));

    mvc.perform(get("/api/cabinet/bell").cookie(psychologist)).andExpect(jsonPath("$.sound").value(false));
    write(put("/api/cabinet/bell"), psychologist, "{\"sound\":true}", 200).andExpect(jsonPath("$.sound").value(true));
    mvc.perform(get("/api/cabinet/bell").cookie(psychologist)).andExpect(jsonPath("$.sound").value(true));
    mvc.perform(get("/api/cabinet/bell").cookie(fourth)).andExpect(status().isForbidden());
  }

  private ResultActions openings(Cookie[] psychologist, LocalDate day) throws Exception {
    return mvc.perform(get("/api/cabinet/schedule/openings").cookie(psychologist).param("from", day.toString()).param("to", day.toString()))
      .andExpect(status().isOk());
  }

  @Test
  void clientBooksSeesAndCancelsSession() throws Exception {
    Cookie[] first = login("first@example.test", PASSWORD);
    Cookie[] second = login("second@example.test", PASSWORD);
    LocalDate day = today().plusDays(1);

    mvc.perform(get("/api/client/offer").param("practice", practiceId()).cookie(first))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.open").value(true))
      .andExpect(jsonPath("$.zone").value("Europe/Moscow"))
      .andExpect(jsonPath("$.types.length()").value(1))
      .andExpect(jsonPath("$.types[0].title").value("Психотерапия очно"));
    slots(first, day).andExpect(jsonPath("$.length()").value(3));

    String start = at(day, "10:00").toString();
    String booked = write(post("/api/client/sessions"), first, "{\"practice\":\"" + practiceId() + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + start + "\"}", 201)
      .andExpect(jsonPath("$.status").value("BOOKED"))
      .andExpect(jsonPath("$.title").value("Психотерапия очно"))
      .andExpect(jsonPath("$.price").value(4500.00))
      .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(booked, "$.id");

    mvc.perform(get("/api/client/sessions").cookie(first)).andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/api/client/sessions").cookie(second)).andExpect(jsonPath("$.length()").value(0));
    slots(second, day).andExpect(jsonPath("$.length()").value(2));
    write(post("/api/client/sessions"), second, "{\"practice\":\"" + practiceId() + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + start + "\"}", 409)
      .andExpect(jsonPath("$.code").value("slot-taken"));

    write(post("/api/client/sessions/" + id + "/cancel"), second, "", 404).andExpect(jsonPath("$.code").value("session-missing"));
    write(post("/api/client/sessions/" + id + "/cancel"), first, "", 200).andExpect(jsonPath("$.status").value("CANCELLED"));
    write(post("/api/client/sessions/" + id + "/cancel"), first, "", 409).andExpect(jsonPath("$.code").value("session-not-active"));
    slots(second, day).andExpect(jsonPath("$.length()").value(3));
    mvc.perform(get("/api/client/sessions").cookie(first)).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void clientCannotBookOutsideOfferOrCancelInsideLead() throws Exception {
    Cookie[] first = login("third@example.test", PASSWORD);
    Cookie[] psychologist = login("booking-psychologist@example.test", "psychologist-password-1");
    LocalDate day = today().plusDays(2);

    write(post("/api/client/sessions"), first, "{\"practice\":\"" + practiceId() + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + at(day, "10:15") + "\"}", 409)
      .andExpect(jsonPath("$.code").value("slot-taken"));
    write(post("/api/client/sessions"), first,
      "{\"practice\":\"" + practiceId() + "\",\"typeId\":\"" + offline + "\",\"start\":\"" + at(day, "10:00") + "\"}", 404)
      .andExpect(jsonPath("$.code").value("session-type-unavailable"));

    String booked = write(post("/api/client/sessions"), first,
        "{\"practice\":\"" + practiceId() + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + at(day, "11:00") + "\"}", 201)
      .andReturn().getResponse().getContentAsString();
    settings(psychologist, 10080);
    write(post("/api/client/sessions/" + JsonPath.read(booked, "$.id") + "/cancel"), first, "", 409)
      .andExpect(jsonPath("$.code").value("cancel-too-late"));

    write(put("/api/cabinet/schedule/settings"), psychologist, "{\"zone\":\"Europe/Moscow\"}", 200);
    mvc.perform(get("/api/client/offer").param("practice", practiceId()).cookie(first)).andExpect(jsonPath("$.open").value(false));
    slots(first, day).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("booking-closed"));
  }

  @Test
  void tenSimultaneousBookingsOfOneSlotGiveOneSession() throws Exception {
    UUID account = clients.enrolAccount(UUID.fromString(practiceId()), accounts.findByEmail("third@example.test").orElseThrow().id());
    Instant start = at(today().plusDays(3), "12:00");
    ExecutorService pool = Executors.newFixedThreadPool(10);
    try {
      List<Callable<Boolean>> attempts = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        attempts.add(() -> {
          try {
            booking.book(UUID.fromString(practiceId()), account, UUID.fromString(therapy), start);
            return true;
          } catch (BookingRefused refused) {
            assertThat(refused.code()).isEqualTo(BookingRefused.SLOT_TAKEN);
            return false;
          }
        });
      }
      int booked = 0;
      for (Future<Boolean> attempt : pool.invokeAll(attempts)) {
        booked += attempt.get() ? 1 : 0;
      }
      assertThat(booked).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void clientReschedulesOwnSession() throws Exception {
    Cookie[] fourth = login("fourth@example.test", PASSWORD);
    LocalDate day = today().plusDays(4);
    String booked = write(post("/api/client/sessions"), fourth, "{\"practice\":\"" + practiceId() + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + at(day, "10:00") + "\"}", 201)
      .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(booked, "$.id");

    write(post("/api/client/sessions/" + id + "/reschedule"), fourth, "{\"start\":\"" + at(day, "10:30") + "\"}", 409)
      .andExpect(jsonPath("$.code").value("slot-taken"));
    write(post("/api/client/sessions/" + id + "/reschedule"), fourth, "{\"start\":\"" + at(day, "12:00") + "\"}", 200)
      .andExpect(jsonPath("$.status").value("BOOKED"))
      .andExpect(jsonPath("$.start").value(at(day, "12:00").toString()));
    mvc.perform(get("/api/client/sessions").cookie(fourth))
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].start").value(at(day, "12:00").toString()));
    slots(fourth, day).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].start").value(at(day, "10:00").toString()));
    write(post("/api/client/sessions/" + id + "/reschedule"), fourth, "{\"start\":\"" + at(day, "11:00") + "\"}", 409)
      .andExpect(jsonPath("$.code").value("session-not-active"));
  }

  @Test
  void clientAreaIsForClientsOnly() throws Exception {
    mvc.perform(get("/api/client/offer").param("practice", practiceId())).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/client/offer").param("practice", practiceId()).cookie(login("booking-psychologist@example.test", "psychologist-password-1")))
      .andExpect(status().isForbidden());
    mvc.perform(get("/api/cabinet/schedule/settings").cookie(login("third@example.test", PASSWORD))).andExpect(status().isForbidden());
  }

  private void openAll(Cookie[] psychologist, LocalDate from, LocalDate to) throws Exception {
    String body = mvc.perform(get("/api/cabinet/schedule/openings").cookie(psychologist).param("from", from.toString())
      .param("to", to.toString())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    java.util.List<String> closed = JsonPath.read(body, "$[?(@.state == 'CLOSED')].start");
    String starts = closed.stream().map(start -> "\"" + start + "\"").collect(java.util.stream.Collectors.joining(","));
    write(put("/api/cabinet/schedule/openings"), psychologist, "{\"open\":[" + starts + "],\"close\":[]}", 204);
  }

  private LocalDate today() {
    return clock.instant().atZone(MOSCOW).toLocalDate();
  }

  private static Instant at(LocalDate day, String time) {
    return ZonedDateTime.of(day, LocalTime.parse(time), MOSCOW).toInstant();
  }

  private void settings(Cookie[] psychologist, int leadMinutes) throws Exception {
    write(put("/api/cabinet/schedule/settings"), psychologist,
      "{\"zone\":\"Europe/Moscow\",\"leadMinutes\":" + leadMinutes + ",\"horizonDays\":30,\"slotStepMinutes\":60}", 200);
  }

  private ResultActions slots(Cookie[] session, LocalDate day) throws Exception {
    return mvc.perform(get("/api/client/slots").cookie(session).param("practice", practiceId()).param("type", therapy).param("from", day.toString())
      .param("to", day.toString()));
  }

  private ResultActions write(MockHttpServletRequestBuilder request, Cookie[] session, String body, int expected) throws Exception {
    return mvc.perform(request.cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().is(expected));
  }

  private Cookie[] login(String email, String password) throws Exception {
    return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
      .andExpect(status().isOk())
      .andReturn().getResponse().getCookies();
  }

  private String type(Cookie[] session, String title, int minutes, int buffer, boolean active) throws Exception {
    String body = "{\"title\":\"" + title + "\",\"durationMinutes\":" + minutes + ",\"bufferMinutes\":" + buffer
      + ",\"price\":\"4500.00\",\"format\":\"IN_PERSON\",\"active\":" + active + "}";
    return JsonPath.read(mvc.perform(post("/api/cabinet/schedule/types").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
  }

  private String practiceId() {
    return accounts.findByEmail("booking-psychologist@example.test").orElseThrow().id().toString();
  }
}
