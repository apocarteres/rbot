package com.yanapaderina.rbot.booking.internal.app;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

// RBOT-OPS-020, ADR-0005
@Component
class SessionMeters {

  private final MeterRegistry registry;

  SessionMeters(MeterRegistry registry) {
    this.registry = registry;
  }

  void changed(String change, boolean byClient) {
    Counter.builder("rbot.sessions.changes").description("Изменения записей: без клиентов и практик в метках")
      .tag("change", change).tag("by", byClient ? "client" : "psychologist").register(registry).increment();
  }
}
