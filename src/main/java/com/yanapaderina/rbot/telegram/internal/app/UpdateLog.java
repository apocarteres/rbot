package com.yanapaderina.rbot.telegram.internal.app;

import com.yanapaderina.rbot.telegram.internal.data.TelegramUpdateDao;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// MVP-04, RBOT-FEAT-009, REQ-DATA-ACCESS-003
@Component
class UpdateLog {

  private final TelegramUpdateDao updates;
  private final Clock clock;

  UpdateLog(TelegramUpdateDao updates, Clock clock) {
    this.updates = updates;
    this.clock = clock;
  }

  @Transactional
  boolean first(long updateId) {
    return updates.remember(updateId, clock.instant());
  }
}
