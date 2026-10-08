package com.yanapaderina.rbot.booking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yanapaderina.rbot.IntegrationStores;
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
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// MVP-05, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-016, RBOT-FEAT-017, ADR-0003
@SpringBootTest(properties = {
  "platform.auth.admin.email=cabinet-psychologist@example.test",
  "platform.auth.admin.password=psychologist-password-1",
  "platform.auth.admin.roles=PSYCHOLOGIST"
})
@AutoConfigureMockMvc
class CabinetSessionsIT extends IntegrationStores {

  private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
  private static String therapy;
  private static final String CLIENT = "visitor@example.test";
  private static final String PASSWORD = "client-password-1";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private Clock clock;

  @Autowired
  private Accounts accounts;

  @Autowired
  private Clients clients;

  @BeforeEach
  void practice() throws Exception {
    if (accounts.findByEmail(CLIENT).isEmpty()) {
      accounts.create(CLIENT, PASSWORD, Set.of("CLIENT"), true, new NoProfile());
    }
    Cookie[] psychologist = psychologist();
    if (therapy == null) {
      therapy = type(psychologist, "Психотерапия очно", 60, 0, true);
    }
    write(put("/api/cabinet/schedule/settings"), psychologist,
      "{\"zone\":\"Europe/Moscow\",\"leadMinutes\":0,\"horizonDays\":30,\"slotStepMinutes\":60}", 200);
    for (int weekday = 1; weekday <= 7; weekday++) {
      write(put("/api/cabinet/schedule/week/" + weekday), psychologist, "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"13:00\",\"types\":[\"" + therapy + "\"]}]}", 200);
    }
  }

  @Test
  void psychologistBooksReschedulesAndCancelsOutsideSchedule() throws Exception {
    Cookie[] psychologist = psychologist();
    String client = clientId();
    LocalDate day = today().plusDays(1);

    mvc.perform(get("/api/cabinet/clients").cookie(psychologist))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.email == '" + CLIENT + "')]").exists());
    String booked = book(psychologist, client, at(day, "20:00"), 201)
      .andExpect(jsonPath("$.clientName").value(CLIENT))
      .andExpect(jsonPath("$.status").value("BOOKED"))
      .andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(booked, "$.id");
    book(psychologist, client, at(day, "20:30"), 409).andExpect(jsonPath("$.code").value("slot-taken"));
    book(psychologist, client, clock.instant().minusSeconds(60), 400).andExpect(jsonPath("$.code").value("session-start-past"));
    String psychologistId = accounts.findByEmail("cabinet-psychologist@example.test").orElseThrow().id().toString();
    book(psychologist, psychologistId, at(day, "08:00"), 404).andExpect(jsonPath("$.code").value("client-missing"));

    String moved = write(post("/api/cabinet/sessions/" + id + "/reschedule"), psychologist, "{\"start\":\"" + at(day, "20:30") + "\"}", 200)
      .andExpect(jsonPath("$.start").value(at(day, "20:30").toString()))
      .andReturn().getResponse().getContentAsString();
    week(psychologist, day)
      .andExpect(jsonPath("$[?(@.id == '" + id + "')].status").value("CANCELLED"))
      .andExpect(jsonPath("$[?(@.id == '" + id + "')].rescheduled").value(true))
      .andExpect(jsonPath("$[?(@.id == '" + JsonPath.read(moved, "$.id") + "')].status").value("BOOKED"));

    write(post("/api/cabinet/sessions/" + JsonPath.read(moved, "$.id") + "/no-show"), psychologist, "", 409)
      .andExpect(jsonPath("$.code").value("session-not-started"));
    write(post("/api/cabinet/sessions/" + JsonPath.read(moved, "$.id") + "/cancel"), psychologist, "", 200)
      .andExpect(jsonPath("$.status").value("CANCELLED"));
    week(psychologist, day).andExpect(jsonPath("$[?(@.id == '" + JsonPath.read(moved, "$.id") + "')].cancelledByClient").value(false));
  }

  @Test
  void clientCancellationIsMarkedAndNoShowFollowsStart() throws Exception {
    Cookie[] psychologist = psychologist();
    Cookie[] visitor = login(CLIENT, PASSWORD);
    LocalDate day = today().plusDays(3);
    String booked = book(psychologist, clientId(), at(day, "21:00"), 201).andReturn().getResponse().getContentAsString();
    write(post("/api/client/sessions/" + JsonPath.read(booked, "$.id") + "/cancel"), visitor, "", 200);
    week(psychologist, day).andExpect(jsonPath("$[?(@.id == '" + JsonPath.read(booked, "$.id") + "')].cancelledByClient").value(true));

    Instant soon = clock.instant().plusSeconds(2).truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
    String started = book(psychologist, clientId(), soon, 201).andReturn().getResponse().getContentAsString();
    while (clock.instant().isBefore(soon.plusMillis(200))) {
      Thread.sleep(50);
    }
    write(post("/api/cabinet/sessions/" + JsonPath.read(started, "$.id") + "/no-show"), psychologist, "", 200)
      .andExpect(jsonPath("$.status").value("NO_SHOW"));
  }

  @Test
  void clientsCannotUseCabinetSessions() throws Exception {
    mvc.perform(get("/api/cabinet/sessions").param("from", today().toString()).param("to", today().toString())
        .cookie(login(CLIENT, PASSWORD)))
      .andExpect(status().isForbidden());
    mvc.perform(get("/api/cabinet/sessions").param("from", today().toString()).param("to", today().plusDays(90).toString())
        .cookie(psychologist()))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("sessions-range-rejected"));
  }

  private String clientId() {
    java.util.UUID practice = accounts.findByEmail("cabinet-psychologist@example.test").orElseThrow().id();
    return clients.enrolAccount(practice, accounts.findByEmail(CLIENT).orElseThrow().id()).toString();
  }

  private LocalDate today() {
    return clock.instant().atZone(MOSCOW).toLocalDate();
  }

  private static Instant at(LocalDate day, String time) {
    return ZonedDateTime.of(day, LocalTime.parse(time), MOSCOW).toInstant();
  }

  private ResultActions book(Cookie[] session, String client, Instant start, int expected) throws Exception {
    return write(post("/api/cabinet/sessions"), session,
      "{\"clientId\":\"" + client + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + start + "\"}", expected);
  }

  private ResultActions week(Cookie[] session, LocalDate day) throws Exception {
    return mvc.perform(get("/api/cabinet/sessions").cookie(session).param("from", day.toString()).param("to", day.toString()))
      .andExpect(status().isOk());
  }

  private Cookie[] psychologist() throws Exception {
    return login("cabinet-psychologist@example.test", "psychologist-password-1");
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
}
