package com.yanapaderina.rbot.bell.internal.web;

import com.yanapaderina.rbot.bell.internal.app.BellPreferences;
import io.github.apocarteres.platform.auth.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RBOT-FEAT-020
@RestController
@RequestMapping("/api/cabinet/bell")
class BellController {

  private final BellPreferences preferences;

  BellController(BellPreferences preferences) {
    this.preferences = preferences;
  }

  @GetMapping
  BellView bell() {
    return new BellView(preferences.sound(CurrentAccount.id().orElseThrow()));
  }

  @PutMapping
  BellView change(@Valid @RequestBody BellView request) {
    return new BellView(preferences.changeSound(CurrentAccount.id().orElseThrow(), request.sound()));
  }

  record BellView(@NotNull Boolean sound) {
  }
}
