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

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-005, ADR-0003
@SpringBootTest(properties = {
  "platform.auth.admin.email=booking-psychologist@example.test",
  "platform.auth.admin.password=psychologist-password-1",
  "platform.auth.admin.roles=PSYCHOLOGIST"
})
@AutoConfigureMockMvc
class ClientBookingIT extends IntegrationStores {

  private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
  private static final String THERAPY = "6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0002";
  private static final String PASSWORD = "client-password-1";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private Clock clock;

  @Autowired
  private Accounts accounts;

  @Autowired
  private ClientBooking booking;

  @BeforeEach
  void practice() throws Exception {
    for (String email : List.of("first@example.test", "second@example.test", "third@example.test", "fourth@example.test")) {
      if (accounts.findByEmail(email).isEmpty()) {
        accounts.create(email, PASSWORD, Set.of("CLIENT"), true, new NoProfile());
      }
    }
    Cookie[] psychologist = login("booking-psychologist@example.test", "psychologist-password-1");
    settings(psychologist, 0);
    for (int weekday = 1; weekday <= 7; weekday++) {
      write(put("/api/cabinet/schedule/week/" + weekday), psychologist, "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"13:00\",\"types\":[\"" + THERAPY + "\"]}]}", 200);
    }
    write(put("/api/cabinet/schedule/types/" + THERAPY), psychologist,
      "{\"title\":\"Психотерапия очно\",\"durationMinutes\":60,\"price\":\"4500.00\",\"format\":\"IN_PERSON\",\"firstVisit\":false,\"active\":true}",
      200);
  }

  @Test
  void clientBooksSeesAndCancelsSession() throws Exception {
    Cookie[] first = login("first@example.test", PASSWORD);
    Cookie[] second = login("second@example.test", PASSWORD);
    LocalDate day = today().plusDays(1);

    mvc.perform(get("/api/client/offer").cookie(first))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.open").value(true))
      .andExpect(jsonPath("$.zone").value("Europe/Moscow"))
      .andExpect(jsonPath("$.types.length()").value(1))
      .andExpect(jsonPath("$.types[0].title").value("Психотерапия очно"));
    slots(first, day).andExpect(jsonPath("$.length()").value(3));

    String start = at(day, "10:00").toString();
    String booked = write(post("/api/client/sessions"), first, "{\"typeId\":\"" + THERAPY + "\",\"start\":\"" + start + "\"}", 201)
      .andExpect(jsonPath("$.status").value("BOOKED"))
      .andExpect(jsonPath("$.title").value("Психотерапия очно"))
      .andExpect(jsonPath("$.price").value(4500.00))
      .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(booked, "$.id");

    mvc.perform(get("/api/client/sessions").cookie(first)).andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/api/client/sessions").cookie(second)).andExpect(jsonPath("$.length()").value(0));
    slots(second, day).andExpect(jsonPath("$.length()").value(2));
    write(post("/api/client/sessions"), second, "{\"typeId\":\"" + THERAPY + "\",\"start\":\"" + start + "\"}", 409)
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

    write(post("/api/client/sessions"), first, "{\"typeId\":\"" + THERAPY + "\",\"start\":\"" + at(day, "10:15") + "\"}", 409)
      .andExpect(jsonPath("$.code").value("slot-taken"));
    write(post("/api/client/sessions"), first,
      "{\"typeId\":\"6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0003\",\"start\":\"" + at(day, "10:00") + "\"}", 404)
      .andExpect(jsonPath("$.code").value("session-type-unavailable"));

    String booked = write(post("/api/client/sessions"), first,
        "{\"typeId\":\"" + THERAPY + "\",\"start\":\"" + at(day, "11:00") + "\"}", 201)
      .andReturn().getResponse().getContentAsString();
    settings(psychologist, 10080);
    write(post("/api/client/sessions/" + JsonPath.read(booked, "$.id") + "/cancel"), first, "", 409)
      .andExpect(jsonPath("$.code").value("cancel-too-late"));

    write(put("/api/cabinet/schedule/settings"), psychologist, "{\"zone\":\"Europe/Moscow\"}", 200);
    mvc.perform(get("/api/client/offer").cookie(first)).andExpect(jsonPath("$.open").value(false));
    slots(first, day).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("booking-closed"));
  }

  @Test
  void tenSimultaneousBookingsOfOneSlotGiveOneSession() throws Exception {
    UUID account = accounts.findByEmail("third@example.test").orElseThrow().id();
    Instant start = at(today().plusDays(3), "12:00");
    ExecutorService pool = Executors.newFixedThreadPool(10);
    try {
      List<Callable<Boolean>> attempts = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        attempts.add(() -> {
          try {
            booking.book(account, UUID.fromString(THERAPY), start);
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
    String booked = write(post("/api/client/sessions"), fourth, "{\"typeId\":\"" + THERAPY + "\",\"start\":\"" + at(day, "10:00") + "\"}", 201)
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
    mvc.perform(get("/api/client/offer")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/client/offer").cookie(login("booking-psychologist@example.test", "psychologist-password-1")))
      .andExpect(status().isForbidden());
    mvc.perform(get("/api/cabinet/schedule/settings").cookie(login("third@example.test", PASSWORD))).andExpect(status().isForbidden());
  }

  private LocalDate today() {
    return clock.instant().atZone(MOSCOW).toLocalDate();
  }

  private static Instant at(LocalDate day, String time) {
    return ZonedDateTime.of(day, LocalTime.parse(time), MOSCOW).toInstant();
  }

  private void settings(Cookie[] psychologist, int leadMinutes) throws Exception {
    write(put("/api/cabinet/schedule/settings"), psychologist,
      "{\"zone\":\"Europe/Moscow\",\"leadMinutes\":" + leadMinutes + ",\"horizonDays\":30,\"slotStepMinutes\":60,\"bufferMinutes\":0}", 200);
  }

  private ResultActions slots(Cookie[] session, LocalDate day) throws Exception {
    return mvc.perform(get("/api/client/slots").cookie(session).param("type", THERAPY).param("from", day.toString())
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
}
