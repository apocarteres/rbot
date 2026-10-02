package com.yanapaderina.rbot.booking.internal.app;

import com.yanapaderina.rbot.clients.PracticeNames;
import com.yanapaderina.rbot.schedule.Availability;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

// RBOT-FEAT-019, RBOT-FEAT-017
@Component
class ScheduledPracticeNames implements PracticeNames {

  private final Availability availability;

  ScheduledPracticeNames(Availability availability) {
    this.availability = availability;
  }

  @Override
  public Optional<String> name(UUID practitioner) {
    return Optional.ofNullable(availability.names(List.of(practitioner)).get(practitioner)).filter(name -> !name.isBlank());
  }
}
