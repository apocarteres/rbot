package com.yanapaderina.rbot.telegram.internal.app;

import com.yanapaderina.rbot.clients.Clients;
import java.util.List;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

// MVP-03, MVP-04, RBOT-FEAT-009, RBOT-FEAT-017, RBOT-FEAT-018, ADR-0002, ADR-0005
@Service
public class UpdateHandler {

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
        onCallback(chat.path("id").asLong(), callback.path("data").asString(""));
      }
    }
  }

  private void onMessage(long chat, long user, String text) {
    String token = text.startsWith("/start ") ? text.substring("/start ".length()).trim() : "";
    if (offer(chat, token)) {
      return;
    }
    if (!clients.byTelegram(user).isEmpty()) {
      bot.sendMessage(chat, texts.text("menu"), app());
      return;
    }
    bot.sendMessage(chat, texts.text(token.isEmpty() ? "invite.needed" : "invite.rejected"), List.of());
  }

  private void onCallback(long chat, String data) {
    if (data.startsWith(CONSENT) && !offer(chat, data.substring(CONSENT.length()))) {
      bot.sendMessage(chat, texts.text("invite.rejected"), List.of());
    }
  }

  private boolean offer(long chat, String token) {
    if (token.isEmpty() || token.length() > 64 || settings.appUrl().isEmpty() || !clients.inviteOpen(token)) {
      return false;
    }
    String url = settings.appUrl() + (settings.appUrl().contains("?") ? "&" : "?") + "invite=" + token;
    bot.sendMessage(chat, texts.text("invite.open"), List.of(BotGateway.Button.app(texts.text("invite.button"), url)));
    return true;
  }

  private List<BotGateway.Button> app() {
    return settings.appUrl().isEmpty() ? List.of() : List.of(BotGateway.Button.app(texts.text("app.button"), settings.appUrl()));
  }
}
