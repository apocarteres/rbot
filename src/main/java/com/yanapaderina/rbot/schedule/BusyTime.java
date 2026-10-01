package com.yanapaderina.rbot.schedule;

import java.util.List;
import java.util.UUID;

// MVP-02, MVP-05, RBOT-FEAT-017, ADR-0003
public interface BusyTime {

  List<TimeRange> busy(UUID practitioner, TimeRange window);
}
