package com.yanapaderina.rbot.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// MVP-02, MVP-05, RBOT-FEAT-002, ADR-0003
public interface Availability {

  Optional<BookingTerms> terms();

  List<SessionType> types();

  List<TimeRange> free(UUID typeId, LocalDate from, LocalDate to);
}
