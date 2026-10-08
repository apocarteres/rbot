package com.yanapaderina.rbot.policy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// RBOT-FEAT-026, ADR-0004
public interface CancellationRules {

  List<CancellationRule> rules(UUID practitioner);

  CancellationDecision decide(UUID practitioner, Instant start);
}
