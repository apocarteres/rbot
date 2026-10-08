package com.yanapaderina.rbot.policy.internal.web;

import com.yanapaderina.rbot.policy.CancellationRule;
import com.yanapaderina.rbot.policy.internal.app.RuleBook;
import io.github.apocarteres.platform.auth.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// RBOT-FEAT-026
@RestController
@RequestMapping("/api/cabinet/cancellation-rules")
class CabinetCancellationRulesController {

  private final RuleBook book;

  CabinetCancellationRulesController(RuleBook book) {
    this.book = book;
  }

  @GetMapping
  List<CancellationRule> rules() {
    return book.rules(practitioner());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  CancellationRule add(@Valid @RequestBody RuleRequest request) {
    return book.add(practitioner(), request.hours(), request.allowed(), request.text());
  }

  @PutMapping("/{id}")
  CancellationRule change(@PathVariable UUID id, @Valid @RequestBody RuleRequest request) {
    return book.change(practitioner(), id, request.hours(), request.allowed(), request.text());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void remove(@PathVariable UUID id) {
    book.remove(practitioner(), id);
  }

  private static UUID practitioner() {
    return CurrentAccount.id().orElseThrow();
  }

  record RuleRequest(@NotNull Integer hours, @NotNull Boolean allowed, @NotNull String text) {
  }
}
