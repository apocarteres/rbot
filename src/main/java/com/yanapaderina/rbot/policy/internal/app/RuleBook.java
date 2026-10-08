package com.yanapaderina.rbot.policy.internal.app;

import com.yanapaderina.rbot.policy.CancellationDecision;
import com.yanapaderina.rbot.policy.CancellationRule;
import com.yanapaderina.rbot.policy.CancellationRules;
import com.yanapaderina.rbot.policy.internal.data.CancellationPolicyDao;
import com.yanapaderina.rbot.policy.internal.data.CancellationRuleDao;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RBOT-FEAT-026, ADR-0004, REQ-DATA-ACCESS-003
@Service
public class RuleBook implements CancellationRules {

  static final int DEFAULT_HOURS = 48;
  static final String DEFAULT_TEXT = "Отмена возможна не позднее чем за 48 часов до начала сессии.";
  static final int MAX_HOURS = 8760;
  static final int MAX_TEXT = 500;

  private final CancellationPolicyDao policies;
  private final CancellationRuleDao rules;
  private final Clock clock;

  RuleBook(CancellationPolicyDao policies, CancellationRuleDao rules, Clock clock) {
    this.policies = policies;
    this.rules = rules;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CancellationRule> rules(UUID practitioner) {
    return policies.exists(practitioner) ? rules.list(practitioner) : List.of(standard(practitioner));
  }

  @Override
  @Transactional(readOnly = true)
  public CancellationDecision decide(UUID practitioner, Instant start) {
    return RuleChoice.decide(rules(practitioner), Duration.between(clock.instant(), start));
  }

  @Transactional
  public CancellationRule add(UUID practitioner, int hours, boolean allowed, String text) {
    configured(practitioner);
    CancellationRule rule = valid(new CancellationRule(UUID.randomUUID(), hours, allowed, text));
    if (!rules.insert(practitioner, rule, clock.instant())) {
      throw taken(hours);
    }
    return rule;
  }

  @Transactional
  public CancellationRule change(UUID practitioner, UUID id, int hours, boolean allowed, String text) {
    configured(practitioner);
    CancellationRule rule = valid(new CancellationRule(id, hours, allowed, text));
    if (rules.find(practitioner, id).isEmpty()) {
      throw missing();
    }
    if (!rules.update(practitioner, rule, clock.instant())) {
      throw taken(hours);
    }
    return rule;
  }

  @Transactional
  public void remove(UUID practitioner, UUID id) {
    configured(practitioner);
    if (!rules.delete(practitioner, id)) {
      throw missing();
    }
  }

  private void configured(UUID practitioner) {
    Instant now = clock.instant();
    if (policies.claim(practitioner, now)) {
      rules.insert(practitioner, standard(practitioner), now);
    }
  }

  private static CancellationRule standard(UUID practitioner) {
    UUID id = UUID.nameUUIDFromBytes(("cancellation-rule:" + practitioner).getBytes(StandardCharsets.UTF_8));
    return new CancellationRule(id, DEFAULT_HOURS, true, DEFAULT_TEXT);
  }

  private static CancellationRule valid(CancellationRule rule) {
    String text = rule.text() == null ? "" : rule.text().strip();
    if (rule.hours() < 0 || rule.hours() > MAX_HOURS || text.isEmpty() || text.length() > MAX_TEXT) {
      throw new PolicyRefused(PolicyRefused.RULE, "Порог или текст правила вне границ");
    }
    return new CancellationRule(rule.id(), rule.hours(), rule.allowed(), text);
  }

  private static PolicyRefused taken(int hours) {
    return new PolicyRefused(PolicyRefused.HOURS_TAKEN, "Правило на " + hours + " ч уже есть");
  }

  private static PolicyRefused missing() {
    return new PolicyRefused(PolicyRefused.MISSING, "Правила нет");
  }
}
