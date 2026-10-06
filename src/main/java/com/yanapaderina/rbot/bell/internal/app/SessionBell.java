package com.yanapaderina.rbot.bell.internal.app;

import com.yanapaderina.rbot.booking.SessionNotice;
import io.github.apocarteres.platform.notifications.Notifications;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// RBOT-FEAT-020, REQ-NOTIFICATIONS-002, REQ-NOTIFICATIONS-003, REQ-NOTIFICATIONS-006, ADR-0005
@Component
class SessionBell {

  private final Notifications notifications;

  SessionBell(Notifications notifications) {
    this.notifications = notifications;
  }

  @EventListener
  void onNotice(SessionNotice notice) {
    if (notice.actor() != SessionNotice.Actor.CLIENT) {
      return;
    }
    Map<String, String> params = new LinkedHashMap<>();
    params.put("client", notice.client().toString());
    params.put("type", notice.title());
    params.put("start", notice.start().toString());
    notice.previousStart().ifPresent(previous -> params.put("previous", previous.toString()));
    notifications.notify(notice.practitioner(), "session." + notice.change().name().toLowerCase(Locale.ROOT), params,
      "/sessions?at=" + notice.start());
  }

  @Scheduled(cron = "0 17 4 * * *")
  void purge() {
    notifications.purgeExpired();
  }
}
