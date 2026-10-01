package com.yanapaderina.rbot.schedule;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yanapaderina.rbot.IntegrationStores;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// MVP-02, RBOT-FEAT-004, RBOT-FEAT-008, RBOT-FEAT-016, ADR-0003
@SpringBootTest(properties = {
  "platform.auth.admin.email=psychologist@example.test",
  "platform.auth.admin.password=psychologist-password-1",
  "platform.auth.admin.roles=PSYCHOLOGIST"
})
@AutoConfigureMockMvc
class ScheduleIT extends IntegrationStores {


  @Autowired
  private MockMvc mvc;

  @Autowired
  private Clock clock;

  @Test
  void psychologistBuildsScheduleAndSeesSlots() throws Exception {
    Cookie[] session = login();
    LocalDate monday = clock.instant().atZone(ZoneId.of("Europe/Moscow")).toLocalDate().plusDays(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    String consultation = type(session, "Разовая консультация", 90, 10, false);
    mvc.perform(get("/api/cabinet/schedule/settings").cookie(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.zone").value("Europe/Moscow"))
      .andExpect(jsonPath("$.complete").value(false));
    mvc.perform(get("/api/cabinet/schedule/slots").cookie(session).param("type", consultation)
        .param("from", monday.toString()).param("to", monday.toString()))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("settings-incomplete"));

    write(put("/api/cabinet/schedule/settings"), session,
      "{\"zone\":\"Europe/Moscow\",\"leadMinutes\":0,\"horizonDays\":30,\"slotStepMinutes\":30}")
      .andExpect(jsonPath("$.complete").value(true));
    write(put("/api/cabinet/schedule/week/1"), session,
      "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"12:00\"},{\"start\":\"11:00\",\"end\":\"13:00\"}]}", 400)
      .andExpect(jsonPath("$.code").value("intervals-overlap"));
    write(put("/api/cabinet/schedule/week/1"), session, "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"13:00\"}]}", 400)
      .andExpect(jsonPath("$.code").value("interval-types-required"));
    write(put("/api/cabinet/schedule/week/1"), session,
      "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"13:00\",\"types\":[\"" + consultation + "\"]}]}");

    mvc.perform(get("/api/cabinet/schedule/slots").cookie(session).param("type", consultation)
        .param("from", monday.toString()).param("to", monday.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(3));

    write(post("/api/cabinet/schedule/days/closed"), session,
      "{\"from\":\"" + monday + "\",\"to\":\"" + monday.plusDays(1) + "\",\"note\":\"отпуск\"}")
      .andExpect(jsonPath("$.days").value(2));
    mvc.perform(get("/api/cabinet/schedule/slots").cookie(session).param("type", consultation)
        .param("from", monday.toString()).param("to", monday.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void sessionTypesAreCreatedEditedAndDeleted() throws Exception {
    Cookie[] session = login();
    String supervision = type(session, "Супервизия", 50, 15, true);
    mvc.perform(get("/api/cabinet/schedule/types").cookie(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.id == '" + supervision + "')].bufferMinutes").value(15));
    write(put("/api/cabinet/schedule/types/" + supervision), session,
      "{\"title\":\"Супервизия\",\"durationMinutes\":50,\"bufferMinutes\":20,\"price\":\"5000.00\",\"format\":\"ONLINE\",\"active\":true}")
      .andExpect(jsonPath("$.bufferMinutes").value(20))
      .andExpect(jsonPath("$.price").value(5000.00));
    write(post("/api/cabinet/schedule/types"), session,
      "{\"title\":\"\",\"durationMinutes\":5,\"bufferMinutes\":0,\"price\":\"-1\",\"format\":\"ONLINE\",\"active\":true}", 400)
      .andExpect(jsonPath("$.code").value("session-type-rejected"));
    write(post("/api/cabinet/schedule/types"), session,
      "{\"title\":\"Долгий перерыв\",\"durationMinutes\":60,\"bufferMinutes\":300,\"price\":\"1\",\"format\":\"ONLINE\",\"active\":true}", 400)
      .andExpect(jsonPath("$.code").value("session-type-rejected"));

    write(put("/api/cabinet/schedule/week/4"), session,
      "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"12:00\",\"types\":[\"" + supervision + "\"]}]}");
    mvc.perform(delete("/api/cabinet/schedule/types/" + supervision).cookie(session).with(csrf())).andExpect(status().isNoContent());
    mvc.perform(delete("/api/cabinet/schedule/types/" + supervision).cookie(session).with(csrf())).andExpect(status().isNotFound());
    mvc.perform(get("/api/cabinet/schedule/types").cookie(session))
      .andExpect(jsonPath("$[?(@.id == '" + supervision + "')]").isEmpty());
    mvc.perform(get("/api/cabinet/schedule/week").cookie(session)).andExpect(jsonPath("$[3].intervals.length()").value(0));
  }

  @Test
  void intervalWithTypesServesOnlyThem() throws Exception {
    Cookie[] session = login();
    String therapy = type(session, "Психотерапия очно", 60, 0, true);
    String consultation = type(session, "Консультация", 90, 0, true);
    LocalDate wednesday = clock.instant().atZone(ZoneId.of("Europe/Moscow")).toLocalDate().plusDays(2)
      .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
    write(put("/api/cabinet/schedule/settings"), session,
      "{\"zone\":\"Europe/Moscow\",\"leadMinutes\":0,\"horizonDays\":30,\"slotStepMinutes\":30}");
    write(put("/api/cabinet/schedule/week/3"), session,
      "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"12:00\",\"types\":[\"6f0d4d1e-8c3b-4b52-9a51-2b1f2a0e0999\"]}]}", 400)
      .andExpect(jsonPath("$.code").value("interval-type-unknown"));
    write(put("/api/cabinet/schedule/week/3"), session,
      "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"12:00\",\"types\":[\"" + therapy + "\"]}]}")
      .andExpect(jsonPath("$[0].types[0]").value(therapy));
    mvc.perform(get("/api/cabinet/schedule/week").cookie(session)).andExpect(jsonPath("$[2].intervals[0].types[0]").value(therapy));
    mvc.perform(get("/api/cabinet/schedule/slots").cookie(session).param("type", therapy)
        .param("from", wednesday.toString()).param("to", wednesday.toString()))
      .andExpect(jsonPath("$.length()").value(3));
    mvc.perform(get("/api/cabinet/schedule/slots").cookie(session).param("type", consultation)
        .param("from", wednesday.toString()).param("to", wednesday.toString()))
      .andExpect(jsonPath("$.length()").value(0));
    write(put("/api/cabinet/schedule/week/3"), session, "{\"intervals\":[]}");
    write(put("/api/cabinet/schedule/settings"), session, "{\"zone\":\"Europe/Moscow\"}");
  }

  @Test
  void malformedRequestsAreRejectedNotFailed() throws Exception {
    Cookie[] session = login();
    write(put("/api/cabinet/schedule/settings"), session, "{\"zone\":", 400).andExpect(jsonPath("$.code").value("request-unreadable"));
    mvc.perform(get("/api/cabinet/schedule/slots").cookie(session).param("type", "not-a-uuid").param("from", "2026-10-05")
        .param("to", "2026-10-05"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("parameter-rejected"));
  }

  @Test
  void guestGetsNoSchedule() throws Exception {
    mvc.perform(get("/api/cabinet/schedule/settings")).andExpect(status().isUnauthorized());
  }

  private org.springframework.test.web.servlet.ResultActions write(
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, Cookie[] session, String body) throws Exception {
    return write(request, session, body, 200);
  }

  private org.springframework.test.web.servlet.ResultActions write(
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, Cookie[] session, String body, int expected)
    throws Exception {
    return mvc.perform(request.cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().is(expected));
  }

  private Cookie[] login() throws Exception {
    return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"psychologist@example.test\",\"password\":\"psychologist-password-1\"}"))
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
