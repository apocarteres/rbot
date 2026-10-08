package com.yanapaderina.rbot.policy.internal.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.yanapaderina.rbot.policy.CancellationDecision;
import com.yanapaderina.rbot.policy.CancellationRule;
import io.github.apocarteres.platform.time.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// RBOT-FEAT-026, ADR-0004
class RuleChoiceTest {

  private static final Instant START = Instant.parse("2026-10-20T07:00:00Z");
  private static final CancellationRule EARLY = new CancellationRule(UUID.randomUUID(), 48, true, "Можно за 48 часов.");
  private static final CancellationRule LATE = new CancellationRule(UUID.randomUUID(), 0, false, "Позже — через психолога.");

  private final MutableClock clock = MutableClock.at(START.minus(Duration.ofDays(2)));

  @Test
  void boundaryBelongsToTheRuleItStarts() {
    assertThat(decide(List.of(LATE, EARLY))).isEqualTo(new CancellationDecision(true, "Можно за 48 часов."));
    clock.advance(Duration.ofSeconds(1));
    assertThat(decide(List.of(LATE, EARLY))).isEqualTo(new CancellationDecision(false, "Позже — через психолога."));
  }

  @Test
  void noMatchingRuleSendsClientToPsychologist() {
    clock.advance(Duration.ofHours(18));
    assertThat(decide(List.of(EARLY))).isEqualTo(new CancellationDecision(false, RuleChoice.CONTACT));
    assertThat(decide(List.of())).isEqualTo(new CancellationDecision(false, RuleChoice.CONTACT));
  }

  @Test
  void startedSessionMatchesNoRule() {
    clock.advance(Duration.ofHours(49));
    assertThat(decide(List.of(EARLY, LATE))).isEqualTo(new CancellationDecision(false, RuleChoice.CONTACT));
  }

  private CancellationDecision decide(List<CancellationRule> rules) {
    return RuleChoice.decide(rules, Duration.between(clock.instant(), START));
  }
}
