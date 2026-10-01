package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.booking.internal.data.SessionDao;
import com.yanapaderina.rbot.schedule.BusyTime;
import com.yanapaderina.rbot.schedule.TimeRange;
import java.util.List;
import org.springframework.stereotype.Component;

// MVP-02, MVP-05, RBOT-FEAT-002, ADR-0003
@Component
class BookedTime implements BusyTime {

  private final SessionDao sessions;

  BookedTime(SessionDao sessions) {
    this.sessions = sessions;
  }

  @Override
  public List<TimeRange> busy(TimeRange window) {
    return sessions.occupied(window.start(), window.end()).stream().map(row -> new TimeRange(row.start(), row.end())).toList();
  }
}
