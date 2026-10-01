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
import com.yanapaderina.rbot.clients.Linking;
import com.yanapaderina.rbot.telegram.internal.app.InitDataSigning;
import io.github.apocarteres.platform.auth.Accounts;
import io.github.apocarteres.platform.auth.NoProfile;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// RBOT-FEAT-017, ADR-0003, ADR-0005
@SpringBootTest(properties = {
  "platform.auth.admin.email=first-psychologist@example.test",
  "platform.auth.admin.password=psychologist-password-1",
  "platform.auth.admin.roles=PSYCHOLOGIST",
  "rbot.telegram.bot-token=" + PracticeIsolationIT.TOKEN,
  "rbot.telegram.bot-username=test_booking_bot",
  "rbot.telegram.polling=false"
})
@AutoConfigureMockMvc
class PracticeIsolationIT extends IntegrationStores {

  static final String TOKEN = "123456:isolation-token-not-real";
  private static final String PASSWORD = "psychologist-password-1";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private Accounts accounts;

  @Autowired
  private Clients clients;

  @Autowired
  private Clock clock;

  @Test
  void practicesDoNotSeeEachOther() throws Exception {
    if (accounts.findByEmail("second-psychologist@example.test").isEmpty()) {
      accounts.create("second-psychologist@example.test", PASSWORD, Set.of("PSYCHOLOGIST"), true, new NoProfile());
    }
    Cookie[] first = login("first-psychologist@example.test");
    Cookie[] second = login("second-psychologist@example.test");

    String firstType = type(first, "Сессия первого");
    String firstInvite = write(post("/api/cabinet/clients"), first, "{\"label\":\"Клиент первого\"}", 201)
      .andReturn().getResponse().getContentAsString();
    write(put("/api/cabinet/schedule/settings"), first,
      "{\"zone\":\"Europe/Moscow\",\"leadMinutes\":0,\"horizonDays\":30,\"slotStepMinutes\":60,\"displayName\":\"Первый психолог\"}", 200);

    mvc.perform(get("/api/cabinet/schedule/types").cookie(second)).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/cabinet/clients").cookie(second)).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/cabinet/schedule/settings").cookie(second))
      .andExpect(jsonPath("$.complete").value(false))
      .andExpect(jsonPath("$.displayName").doesNotExist());
    write(put("/api/cabinet/schedule/week/1"), second,
      "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"12:00\",\"types\":[\"" + firstType + "\"]}]}", 400)
      .andExpect(jsonPath("$.code").value("interval-type-unknown"));
    write(put("/api/cabinet/schedule/types/" + firstType), second,
      "{\"title\":\"Чужой\",\"durationMinutes\":60,\"bufferMinutes\":0,\"price\":\"1\",\"format\":\"ONLINE\",\"active\":true}", 404);
    String firstClient = JsonPath.read(firstInvite, "$.clientId");
    write(post("/api/cabinet/clients/" + firstClient + "/invite"), second, "", 404);

    String secondInvite = write(post("/api/cabinet/clients"), second, "{\"label\":\"Клиент второго\"}", 201)
      .andReturn().getResponse().getContentAsString();
    long user = 9001;
    Assertions.assertThat(clients.link(token(firstInvite), user, 1)).isInstanceOf(Linking.Linked.class);
    Assertions.assertThat(clients.link(token(secondInvite), user, 1)).isInstanceOf(Linking.Linked.class);
    mvc.perform(get("/api/miniapp/practices").header("X-Telegram-Init-Data", InitDataSigning.signed(TOKEN, user, clock.instant())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[?(@.name == 'Первый психолог')]").exists())
      .andExpect(jsonPath("$[?(@.name == 'Психолог')]").exists());
    mvc.perform(get("/api/cabinet/clients").cookie(first))
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].name").value("Клиент первого"));
  }

  private static String token(String invite) {
    String link = JsonPath.read(invite, "$.link");
    return link.substring(link.indexOf("start=") + 6);
  }

  private String type(Cookie[] session, String title) throws Exception {
    return JsonPath.read(write(post("/api/cabinet/schedule/types"), session,
      "{\"title\":\"" + title + "\",\"durationMinutes\":60,\"bufferMinutes\":0,\"price\":\"1000\",\"format\":\"ONLINE\",\"active\":true}", 201)
      .andReturn().getResponse().getContentAsString(), "$.id");
  }

  private ResultActions write(MockHttpServletRequestBuilder request, Cookie[] session, String body, int expected) throws Exception {
    return mvc.perform(request.cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().is(expected));
  }

  private Cookie[] login(String email) throws Exception {
    return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
      .andExpect(status().isOk())
      .andReturn().getResponse().getCookies();
  }
}
