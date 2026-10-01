package com.yanapaderina.rbot.telegram.internal.app;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

// MVP-04, RBOT-FEAT-009, ADR-0001, REQ-AUTH-040, REQ-CONFIG
@Component
class RestBotGateway implements BotGateway {

  private static final Log LOG = LogFactory.getLog(RestBotGateway.class);

  private final TelegramSettings settings;
  private final RestClient rest;

  RestBotGateway(TelegramSettings settings) {
    this.settings = settings;
    HttpClient.Builder http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5));
    if (!settings.proxy().isEmpty()) {
      int split = settings.proxy().lastIndexOf(':');
      http.proxy(ProxySelector.of(new InetSocketAddress(settings.proxy().substring(0, split),
        Integer.parseInt(settings.proxy().substring(split + 1)))));
    }
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http.build());
    factory.setReadTimeout(Duration.ofSeconds(40));
    this.rest = RestClient.builder().requestFactory(factory).baseUrl(settings.apiBase()).build();
  }

  @Override
  public void sendMessage(long chatId, String text, List<Button> buttons) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("chat_id", chatId);
    body.put("text", text);
    if (!buttons.isEmpty()) {
      body.put("reply_markup", Map.of("inline_keyboard", buttons.stream().map(button -> List.of(markup(button))).toList()));
    }
    call("sendMessage", body);
  }

  @Override
  public void answerCallback(String callbackId) {
    call("answerCallbackQuery", Map.of("callback_query_id", callbackId));
  }

  @Override
  public List<JsonNode> updates(long offset, int waitSeconds) {
    if (!settings.configured()) {
      return List.of();
    }
    try {
      JsonNode answer = rest.post().uri("/bot{token}/getUpdates", settings.botToken()).contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("offset", offset, "timeout", waitSeconds, "allowed_updates", List.of("message", "callback_query")))
        .retrieve().body(JsonNode.class);
      List<JsonNode> updates = new ArrayList<>();
      if (answer != null) {
        answer.path("result").forEach(updates::add);
      }
      return updates;
    } catch (RestClientException failed) {
      LOG.warn("Telegram getUpdates отказал: " + failed.getClass().getSimpleName());
      return List.of();
    }
  }

  private void call(String method, Map<String, Object> body) {
    if (!settings.configured()) {
      LOG.warn("Сообщение Telegram " + method + " не отправлено: бот не настроен");
      return;
    }
    try {
      rest.post().uri("/bot{token}/{method}", settings.botToken(), method).contentType(MediaType.APPLICATION_JSON).body(body)
        .retrieve().toBodilessEntity();
    } catch (RestClientException failed) {
      LOG.warn("Telegram " + method + " отказал: " + failed.getClass().getSimpleName());
    }
  }

  private static Map<String, Object> markup(Button button) {
    return button.webAppUrl() != null
      ? Map.of("text", button.text(), "web_app", Map.of("url", button.webAppUrl()))
      : Map.of("text", button.text(), "callback_data", button.callbackData());
  }
}
