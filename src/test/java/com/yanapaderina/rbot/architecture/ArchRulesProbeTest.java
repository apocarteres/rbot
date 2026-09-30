package com.yanapaderina.rbot.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.apocarteres.platform.arch.PlatformArchRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// RBOT-ARC-001, REQ-JAVA-MODULES-003, REQ-JAVA-MODULES-006
class ArchRulesProbeTest {

  private static final String FIXTURE = "com.yanapaderina.rbot.architecture.fixture";

  private final JavaClasses fixtures = new ClassFileImporter().importPackages(FIXTURE);

  @Test
  @DisplayName("Косвенный цикл a → b → c → a роняет проверку модулей")
  void indirectCycleIsFound() {
    assertThatThrownBy(() -> PlatformArchRules.modulesAreFreeOfCycles(FIXTURE + ".indirect").check(fixtures))
      .isInstanceOf(AssertionError.class);
  }

  @Test
  @DisplayName("Цикл между подмодулями x ↔ y внутри модуля роняет проверку подмодулей")
  void nestedCycleIsFound() {
    assertThatThrownBy(() -> PlatformArchRules.submodulesAreFreeOfCycles(FIXTURE + ".nested.module").check(fixtures))
      .isInstanceOf(AssertionError.class);
  }
}
