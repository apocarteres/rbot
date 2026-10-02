package com.yanapaderina.rbot.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yanapaderina.rbot.IntegrationStores;
import com.yanapaderina.rbot.telegram.internal.app.BotGateway;
import com.yanapaderina.rbot.telegram.internal.app.InitDataSigning;
import com.yanapaderina.rbot.telegram.internal.data.PollingLeaseDao;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// MVP-03, MVP-04, MVP-08, RBOT-FEAT-009, RBOT-FEAT-016, RBOT-FEAT-017, RBOT-FEAT-018, RBOT-OPS-018, ADR-0002, ADR-0005
@SpringBootTest(properties = {
  "platform.auth.admin.email=telegram-psychologist@example.test",
  "platform.auth.admin.password=psychologist-password-1",
  "platform.auth.admin.roles=PSYCHOLOGIST",
  "rbot.telegram.bot-token=" + TelegramIT.TOKEN,
  "rbot.telegram.bot-username=test_booking_bot",
  "rbot.telegram.webhook-secret=" + TelegramIT.SECRET,
  "rbot.telegram.app-url=https://bot.example.test/",
  "rbot.telegram.polling=false"
})
@AutoConfigureMockMvc
class TelegramIT extends IntegrationStores {

  static final String TOKEN = "123456:integration-token-not-real";
  static final String SECRET = "webhook-secret-for-tests";
  private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
  private static final AtomicLong UPDATES = new AtomicLong(1000);

  @Autowired
  private MockMvc mvc;

  @Autowired
  private Clock clock;

  @Autowired
  private JdbcClient jdbc;

  @Autowired
  private PollingLeaseDao lease;

  @MockitoBean
  private BotGateway bot;

  @Test
  void webhookWithoutSecretIsRejected() throws Exception {
    mvc.perform(post("/api/tg/webhook").contentType(MediaType.APPLICATION_JSON).content(start(1, "")))
      .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/tg/webhook").header("X-Telegram-Bot-Api-Secret-Token", "wrong").contentType(MediaType.APPLICATION_JSON)
        .content(start(1, "")))
      .andExpect(status().isUnauthorized());
    verify(bot, never()).sendMessage(anyLong(), anyString(), anyList());
  }

  @Test
  void invitedClientLinksTelegramAndBooksInMiniApp() throws Exception {
    Cookie[] psychologist = psychologist();
    String invite = write(post("/api/cabinet/clients"), psychologist, "{\"label\":\"Анна П.\"}", 201)
      .andReturn().getResponse().getContentAsString();
    String link = JsonPath.read(invite, "$.link");
    assertThat(link).startsWith("https://t.me/test_booking_bot?start=");
    String token = link.substring(link.indexOf("start=") + 6);
    assertThat(jdbc.sql("SELECT count(*) FROM client_invite WHERE token_hash = :token").param("token", token).query(Long.class).single())
      .isZero();

    long user = 7001;
    String miniApp = InitDataSigning.signed(TOKEN, user, clock.instant());
    mvc.perform(get("/api/miniapp/sessions").header("X-Telegram-Init-Data", miniApp))
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.code").value("client-not-linked"));

    webhook(start(user, token));
    ArgumentCaptor<java.util.List<BotGateway.Button>> buttons = buttons();
    verify(bot).sendMessage(eq(user), anyString(), buttons.capture());
    assertThat(buttons.getValue().getFirst().webAppUrl()).isEqualTo("https://bot.example.test/?invite=" + token);

    String tokenBody = "{\"token\":\"" + token + "\"}";
    miniApp(post("/api/miniapp/invitation"), miniApp, tokenBody, 200)
      .andExpect(jsonPath("$.version").value(1))
      .andExpect(jsonPath("$.practiceName").value("Психолог"))
      .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.startsWith("Я соглашаюсь")));
    miniApp(post("/api/miniapp/invitation/accept"), miniApp, "{\"token\":\"" + token + "\",\"version\":2}", 409)
      .andExpect(jsonPath("$.code").value("consent-outdated"));
    miniApp(post("/api/miniapp/invitation/accept"), miniApp, "{\"token\":\"" + token + "\",\"version\":1}", 204);
    miniApp(post("/api/miniapp/invitation/accept"), miniApp, "{\"token\":\"" + token + "\",\"version\":1}", 404)
      .andExpect(jsonPath("$.code").value("invite-rejected"));
    clearInvocations(bot);
    webhook(consent(user, token));
    verify(bot).sendMessage(eq(user), eq("Ссылка-приглашение не действует: она уже использована или устарела. Попросите у психолога новую."),
      anyList());

    write(put("/api/cabinet/schedule/settings"), psychologist,
      "{\"zone\":\"Europe/Moscow\",\"leadMinutes\":0,\"horizonDays\":30,\"slotStepMinutes\":60}", 200);
    String therapy = type(psychologist, "Психотерапия очно", 60, 0, true);
    String practice = JsonPath.read(mvc.perform(get("/api/miniapp/practices").header("X-Telegram-Init-Data", miniApp))
      .andExpect(jsonPath("$.length()").value(1)).andReturn().getResponse().getContentAsString(), "$[0].id");
    LocalDate day = clock.instant().atZone(MOSCOW).toLocalDate().plusDays(1);
    write(put("/api/cabinet/schedule/week/" + day.getDayOfWeek().getValue()), psychologist,
      "{\"intervals\":[{\"start\":\"10:00\",\"end\":\"13:00\",\"types\":[\"" + therapy + "\"]}]}", 200);
    String start = ZonedDateTime.of(day, LocalTime.parse("11:00"), MOSCOW).toInstant().toString();
    mvc.perform(post("/api/miniapp/sessions").header("X-Telegram-Init-Data", miniApp).contentType(MediaType.APPLICATION_JSON)
        .content("{\"practice\":\"" + practice + "\",\"typeId\":\"" + therapy + "\",\"start\":\"" + start + "\"}"))
      .andExpect(status().isCreated());
    mvc.perform(get("/api/miniapp/sessions").header("X-Telegram-Init-Data", miniApp))
      .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/api/miniapp/sessions").header("X-Telegram-Init-Data", InitDataSigning.signed("999:forged", user, clock.instant())))
      .andExpect(status().isUnauthorized());

    String sessions = mvc.perform(get("/api/cabinet/sessions").cookie(psychologist).param("from", day.toString()).param("to", day.toString()))
      .andExpect(jsonPath("$[0].clientName").value("Анна П."))
      .andReturn().getResponse().getContentAsString();
    clearInvocations(bot);
    write(post("/api/cabinet/sessions/" + JsonPath.read(sessions, "$[0].id") + "/cancel"), psychologist, "", 200);
    verify(bot).sendMessage(eq(user), org.mockito.ArgumentMatchers.startsWith("Психолог отменил вашу сессию «Психотерапия очно»"), anyList());

    mvc.perform(get("/api/cabinet/consent").cookie(psychologist))
      .andExpect(jsonPath("$.version").value(1))
      .andExpect(jsonPath("$.savedAt").isNotEmpty());
    write(put("/api/cabinet/consent"), psychologist, "{\"body\":\"  \"}", 400)
      .andExpect(jsonPath("$.code").value("consent-text-rejected"));
    write(put("/api/cabinet/consent"), psychologist, "{\"body\":\"Новый текст согласия\"}", 200)
      .andExpect(jsonPath("$.version").value(2));
    mvc.perform(get("/api/miniapp/practices").header("X-Telegram-Init-Data", miniApp))
      .andExpect(jsonPath("$.code").value("client-not-linked"));
    mvc.perform(get("/api/miniapp/consents").header("X-Telegram-Init-Data", miniApp))
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].version").value(2))
      .andExpect(jsonPath("$[0].text").value("Новый текст согласия"));
    miniApp(post("/api/miniapp/consents"), miniApp, "{\"practice\":\"" + practice + "\",\"version\":1}", 409);
    miniApp(post("/api/miniapp/consents"), miniApp, "{\"practice\":\"" + practice + "\",\"version\":2}", 204);
    mvc.perform(get("/api/miniapp/consents").header("X-Telegram-Init-Data", miniApp))
      .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/miniapp/practices").header("X-Telegram-Init-Data", miniApp))
      .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void legacyConsentButtonOpensApp() throws Exception {
    String invite = write(post("/api/cabinet/clients"), psychologist(), "{\"label\":\"Борис К.\"}", 201)
      .andReturn().getResponse().getContentAsString();
    String link = JsonPath.read(invite, "$.link");
    String token = link.substring(link.indexOf("start=") + 6);
    webhook(consent(7004, token));
    ArgumentCaptor<java.util.List<BotGateway.Button>> buttons = buttons();
    verify(bot).sendMessage(eq(7004L), anyString(), buttons.capture());
    assertThat(buttons.getValue().getFirst().webAppUrl()).isEqualTo("https://bot.example.test/?invite=" + token);
    assertThat(jdbc.sql("SELECT count(*) FROM client WHERE telegram_user_id = 7004").query(Long.class).single()).isZero();
  }

  @Test
  void spentInviteIsNotOffered() throws Exception {
    webhook(start(7003, "unknown-token"));
    verify(bot).sendMessage(eq(7003L), org.mockito.ArgumentMatchers.startsWith("Ссылка-приглашение не действует"), anyList());
  }

  @Test
  void strangerWithoutInviteIsAskedForOne() throws Exception {
    webhook(start(7002, ""));
    verify(bot).sendMessage(eq(7002L), org.mockito.ArgumentMatchers.startsWith("Здравствуйте! Запись к психологу открывается по приглашению"),
      anyList());
  }

  @Test
  void laterInstanceTakesPollingLease() {
    Instant now = clock.instant();
    Instant stale = now.minusSeconds(75);
    jdbc.sql("DELETE FROM telegram_polling_lease").update();
    assertThat(lease.claim("old", now.minusSeconds(3600), now, stale)).isTrue();
    assertThat(lease.claim("new", now.minusSeconds(60), now, stale)).isTrue();
    assertThat(lease.claim("old", now.minusSeconds(3600), now.plusSeconds(1), stale)).isFalse();
    assertThat(lease.claim("new", now.minusSeconds(60), now.plusSeconds(1), stale)).isTrue();
    assertThat(lease.claim("old", now.minusSeconds(3600), now.plusSeconds(200), now.plusSeconds(125))).isTrue();
    assertThat(lease.release("old")).isTrue();
    assertThat(lease.claim("other", now.minusSeconds(7200), now.plusSeconds(201), now.plusSeconds(126))).isTrue();
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<java.util.List<BotGateway.Button>> buttons() {
    return ArgumentCaptor.forClass(java.util.List.class);
  }

  private static String start(long user, String token) {
    return "{\"update_id\":" + UPDATES.incrementAndGet() + ",\"message\":{\"message_id\":1,\"from\":{\"id\":" + user
      + "},\"chat\":{\"id\":" + user + ",\"type\":\"private\"},\"text\":\"/start" + (token.isEmpty() ? "" : " " + token) + "\"}}";
  }

  private static String consent(long user, String token) {
    return "{\"update_id\":" + UPDATES.incrementAndGet() + ",\"callback_query\":{\"id\":\"cb\",\"from\":{\"id\":" + user
      + "},\"message\":{\"message_id\":2,\"chat\":{\"id\":" + user + ",\"type\":\"private\"}},\"data\":\"c:" + token + "\"}}";
  }

  private void webhook(String body) throws Exception {
    mvc.perform(post("/api/tg/webhook").header("X-Telegram-Bot-Api-Secret-Token", SECRET).contentType(MediaType.APPLICATION_JSON)
        .content(body))
      .andExpect(status().isOk());
  }

  private ResultActions miniApp(MockHttpServletRequestBuilder request, String initData, String body, int expected) throws Exception {
    return mvc.perform(request.header("X-Telegram-Init-Data", initData).contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().is(expected));
  }

  private Cookie[] psychologist() throws Exception {
    return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"telegram-psychologist@example.test\",\"password\":\"psychologist-password-1\"}"))
      .andExpect(status().isOk())
      .andReturn().getResponse().getCookies();
  }

  private ResultActions write(MockHttpServletRequestBuilder request, Cookie[] session, String body, int expected) throws Exception {
    return mvc.perform(request.cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().is(expected));
  }

  private String type(Cookie[] session, String title, int minutes, int buffer, boolean active) throws Exception {
    String body = "{\"title\":\"" + title + "\",\"durationMinutes\":" + minutes + ",\"bufferMinutes\":" + buffer
      + ",\"price\":\"4500.00\",\"format\":\"IN_PERSON\",\"active\":" + active + "}";
    return JsonPath.read(mvc.perform(post("/api/cabinet/schedule/types").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
  }
}
