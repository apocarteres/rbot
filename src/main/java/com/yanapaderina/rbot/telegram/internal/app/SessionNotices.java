package com.yanapaderina.rbot.telegram.internal.app;

import com.yanapaderina.rbot.booking.SessionNotice;
import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.schedule.Availability;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// MVP-04, MVP-05, RBOT-FEAT-009, ADR-0001, REQ-AUTH-040
@Component
class SessionNotices {

  private static final Log LOG = LogFactory.getLog(SessionNotices.class);
  private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EE, d MMMM, HH:mm", Locale.forLanguageTag("ru"));

  private final Clients clients;
  private final Availability availability;
  private final BotGateway bot;
  private final TelegramTexts texts;
  private final TelegramSettings settings;

  SessionNotices(Clients clients, Availability availability, BotGateway bot, TelegramTexts texts, TelegramSettings settings) {
    this.clients = clients;
    this.availability = availability;
    this.bot = bot;
    this.texts = texts;
    this.settings = settings;
  }

  @TransactionalEventListener
  void onNotice(SessionNotice notice) {
    try {
      clients.telegramOf(notice.client()).ifPresent(chat -> bot.sendMessage(chat, text(notice, availability.zone()), app()));
    } catch (RuntimeException failed) {
      LOG.warn("Уведомление о сессии не отправлено: " + failed.getClass().getSimpleName());
    }
  }

  private String text(SessionNotice notice, ZoneId zone) {
    return switch (notice.change()) {
      case BOOKED -> texts.text("notice.booked", notice.title(), when(notice.start(), zone));
      case RESCHEDULED -> texts.text("notice.rescheduled", notice.title(), when(notice.previousStart().orElse(notice.start()), zone),
        when(notice.start(), zone));
      case CANCELLED -> texts.text("notice.cancelled", notice.title(), when(notice.start(), zone));
    };
  }

  private static String when(Instant instant, ZoneId zone) {
    return WHEN.format(instant.atZone(zone));
  }

  private List<BotGateway.Button> app() {
    return settings.appUrl().isEmpty() ? List.of() : List.of(BotGateway.Button.app(texts.text("app.button"), settings.appUrl()));
  }
}
