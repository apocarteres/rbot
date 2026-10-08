package com.yanapaderina.rbot.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import io.github.apocarteres.platform.arch.PlatformArchRules;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// RBOT-ARC-001, RBOT-DATA-001, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-026, REQ-JAVA-MODULES-001, REQ-JAVA-MODULES-002, REQ-JAVA-MODULES-003, REQ-DATA-ACCESS-001, REQ-DATA-ACCESS-002, REQ-DATA-ACCESS-003, REQ-DATA-ACCESS-005
class PlatformArchRulesTest {

  private static final String ROOT = "com.yanapaderina.rbot";

  static final Map<String, Set<String>> MODULE_DEPENDENCIES = Map.of(
    "access", Set.of(),
    "bell", Set.of("booking"),
    "accounts", Set.of("access"),
    "booking", Set.of("access", "clients", "policy", "schedule"),
    "clients", Set.of(),
    "policy", Set.of(),
    "schedule", Set.of(),
    "telegram", Set.of("access", "booking", "clients", "schedule"),
    "web", Set.of());

  private static final DescribedPredicate<JavaClass> DATA_ACCESS =
    DescribedPredicate.describe("лежат в пакете internal.data", type -> type.getPackageName().endsWith(".internal.data"));

  private static final DescribedPredicate<JavaClass> APPLICATION_LAYER =
    DescribedPredicate.describe("лежат в пакете internal.app", type -> type.getPackageName().endsWith(".internal.app"));

  private final JavaClasses classes = new ClassFileImporter()
    .withImportOption(new ImportOption.DoNotIncludeTests())
    .importPackages("com.yanapaderina");

  @Test
  @DisplayName("Граф модулей и подмодулей ациклический, циклы не скрыты")
  void modulesAreAcyclic() {
    PlatformArchRules.modulesAreFreeOfCycles(ROOT).check(classes);
    MODULE_DEPENDENCIES.keySet().forEach(module -> PlatformArchRules.submodulesAreFreeOfCycles(ROOT + "." + module).check(classes));
    PlatformArchRules.cyclesAreNotHidden().check(classes);
  }

  @Test
  @DisplayName("Модуль зависит только от модулей своего перечня")
  void modulesDependOnlyOnDeclared() {
    MODULE_DEPENDENCIES.forEach((module, allowed) -> MODULE_DEPENDENCIES.keySet().stream()
      .filter(other -> !other.equals(module) && !allowed.contains(other))
      .forEach(other -> ArchRuleDefinition.noClasses().that().resideInAPackage(ROOT + "." + module + "..")
        .should().dependOnClassesThat().resideInAPackage(ROOT + "." + other + "..")
        .check(classes)));
  }

  @Test
  @DisplayName("Подпакет internal чужого модуля не используется")
  void internalsStayInside() {
    PlatformArchRules.innerPackagesStayInside(ROOT, "internal").check(classes);
  }

  @Test
  @DisplayName("Доступ к данным: без ORM, один оператор в методе, каталог SQL только в слое доступа")
  void dataAccess() {
    PlatformArchRules.objectRelationalMappingIsNotUsed().check(classes);
    PlatformArchRules.dataAccessMethodsRunOneStatement(DATA_ACCESS).check(classes);
    PlatformArchRules.sqlCatalogueIsUsedOnlyBy(DATA_ACCESS).check(classes);
  }

  @Test
  @DisplayName("Транзакции объявляются только в прикладном слое")
  void transactions() {
    PlatformArchRules.transactionsAreDeclaredOnlyIn(APPLICATION_LAYER).check(classes);
  }
}
