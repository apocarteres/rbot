package com.yanapaderina.rbot.policy.internal.app;

import com.yanapaderina.rbot.policy.CancellationDecision;
import com.yanapaderina.rbot.policy.CancellationRule;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

// RBOT-FEAT-026, ADR-0004, REQ-CODE-DESIGN-003
final class RuleChoice {

  static final String CONTACT = "Для отмены обратитесь к психологу.";

  private RuleChoice() {
  }

  static CancellationDecision decide(List<CancellationRule> rules, Duration left) {
    return rules.stream()
      .filter(rule -> Duration.ofHours(rule.hours()).compareTo(left) <= 0)
      .max(Comparator.comparingInt(CancellationRule::hours))
      .map(rule -> new CancellationDecision(rule.allowed(), rule.text()))
      .orElseGet(() -> new CancellationDecision(false, CONTACT));
  }
}
