package com.yanapaderina.rbot.clients.internal.web;

import com.yanapaderina.rbot.clients.Clients;
import com.yanapaderina.rbot.clients.ConsentText;
import io.github.apocarteres.platform.auth.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// MVP-03, RBOT-FEAT-018, ADR-0005
@RestController
@RequestMapping("/api/cabinet/consent")
class CabinetConsentController {

  private final Clients clients;

  CabinetConsentController(Clients clients) {
    this.clients = clients;
  }

  @GetMapping
  ConsentView current() {
    return ConsentView.of(clients.consentText(CurrentAccount.id().orElseThrow()));
  }

  @PutMapping
  ConsentView save(@Valid @RequestBody ConsentRequest request) {
    return ConsentView.of(clients.saveConsentText(CurrentAccount.id().orElseThrow(), request.body()));
  }

  record ConsentView(Integer version, String body, Instant savedAt) {

    static ConsentView of(ConsentText text) {
      return new ConsentView(text.savedAt().map(at -> text.version()).orElse(null), text.body(), text.savedAt().orElse(null));
    }
  }

  record ConsentRequest(@NotNull String body) {
  }
}
