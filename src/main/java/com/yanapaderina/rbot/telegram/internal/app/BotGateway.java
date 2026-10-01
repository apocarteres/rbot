package com.yanapaderina.rbot.telegram.internal.app;

import java.util.List;

// MVP-04, RBOT-FEAT-009, ADR-0001
public interface BotGateway {

  void sendMessage(long chatId, String text, List<Button> buttons);

  void answerCallback(String callbackId);

  record Button(String text, String callbackData, String webAppUrl) {

    public static Button callback(String text, String data) {
      return new Button(text, data, null);
    }

    public static Button app(String text, String url) {
      return new Button(text, null, url);
    }
  }
}
