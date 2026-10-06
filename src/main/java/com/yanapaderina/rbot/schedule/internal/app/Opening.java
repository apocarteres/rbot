package com.yanapaderina.rbot.schedule.internal.app;

import java.time.Instant;

// RBOT-FEAT-021, REQ-CODE-DESIGN-001
public record Opening(Instant start, State state) {

  public enum State {
    OPEN,
    CLOSED,
    BUSY
  }
}
