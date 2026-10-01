package com.yanapaderina.rbot.schedule;

import java.util.List;

// MVP-02, MVP-05, ADR-0003
public interface BusyTime {

  List<TimeRange> busy(TimeRange window);
}
