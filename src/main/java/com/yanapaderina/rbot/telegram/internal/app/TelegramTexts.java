package com.yanapaderina.rbot.telegram.internal.app;

import java.util.Locale;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

// MVP-04, RBOT-FEAT-009
@Component
class TelegramTexts {

  private static final Locale RUSSIAN = Locale.forLanguageTag("ru");

  private final ResourceBundleMessageSource source = new ResourceBundleMessageSource();

  TelegramTexts() {
    source.setBasename("telegram/messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
  }

  String text(String key, Object... arguments) {
    return source.getMessage(key, arguments, RUSSIAN);
  }
}
