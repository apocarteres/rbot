package com.yanapaderina.rbot.schedule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// MVP-02, MVP-05, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-016, RBOT-FEAT-017, ADR-0003
public interface Availability {

  ZoneId zone(UUID practitioner);

  Optional<BookingTerms> terms(UUID practitioner);

  List<SessionType> types(UUID practitioner);

  List<SessionType> everyType(UUID practitioner);

  List<TimeRange> free(UUID practitioner, UUID typeId, LocalDate from, LocalDate to);

  Map<UUID, String> names(Collection<UUID> practitioners);
}
