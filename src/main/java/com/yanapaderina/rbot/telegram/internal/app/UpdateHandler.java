package com.yanapaderina.rbot.telegram.internal.app;

import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.clients.Linking;
import java.util.List;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

// MVP-03, MVP-04, RBOT-FEAT-009, RBOT-FEAT-017, ADR-0002, ADR-0005
@Service
public class UpdateHandler {

  static final int CONSENT_VERSION = 1;
  private static final String CONSENT = "c:";

  private final UpdateLog log;
  private final Clients clients;
  private final BotGateway bot;
  private final TelegramTexts texts;
  private final TelegramSettings settings;

  UpdateHandler(UpdateLog log, Clients clients, BotGateway bot, TelegramTexts texts, TelegramSettings settings) {
    this.log = log;
    this.clients = clients;
    this.bot = bot;
    this.texts = texts;
    this.settings = settings;
  }

  public void handle(JsonNode update) {
    long updateId = update.path("update_id").asLong(-1);
    if (updateId < 0 || !log.first(updateId)) {
      return;
    }
    JsonNode message = update.path("message");
    JsonNode callback = update.path("callback_query");
    if (!message.isMissingNode() && "private".equals(message.path("chat").path("type").asString(""))) {
      onMessage(message.path("chat").path("id").asLong(), message.path("from").path("id").asLong(), message.path("text").asString(""));
    } else if (!callback.isMissingNode()) {
      bot.answerCallback(callback.path("id").asString(""));
      JsonNode chat = callback.path("message").path("chat");
      if ("private".equals(chat.path("type").asString(""))) {
        onCallback(chat.path("id").asLong(), callback.path("from").path("id").asLong(), callback.path("data").asString(""));
      }
    }
  }

  private void onMessage(long chat, long user, String text) {
    String token = text.startsWith("/start ") ? text.substring("/start ".length()).trim() : "";
    if (!token.isEmpty() && token.length() <= 64 && clients.inviteOpen(token)) {
      bot.sendMessage(chat, texts.text("consent.request", texts.text("consent.text")),
        List.of(BotGateway.Button.callback(texts.text("consent.button"), CONSENT + token)));
      return;
    }
    if (!clients.byTelegram(user).isEmpty()) {
      bot.sendMessage(chat, texts.text("menu"), app());
      return;
    }
    bot.sendMessage(chat, texts.text(token.isEmpty() ? "invite.needed" : "invite.rejected"), List.of());
  }

  private void onCallback(long chat, long user, String data) {
    if (!data.startsWith(CONSENT)) {
      return;
    }
    Linking result = clients.link(data.substring(CONSENT.length()), user, CONSENT_VERSION);
    switch (result) {
      case Linking.Linked linked -> bot.sendMessage(chat, texts.text("linked"), app());
      case Linking.InviteRejected rejected -> bot.sendMessage(chat, texts.text("invite.rejected"), List.of());
      case Linking.TelegramTaken taken -> bot.sendMessage(chat, texts.text("telegram.taken"), List.of());
    }
  }

  private List<BotGateway.Button> app() {
    return settings.appUrl().isEmpty() ? List.of() : List.of(BotGateway.Button.app(texts.text("app.button"), settings.appUrl()));
  }
}
