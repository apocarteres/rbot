package com.yanapaderina.rbot.telegram.internal.web;

import com.yanapaderina.rbot.telegram.internal.app.UpdateHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

// MVP-04, RBOT-FEAT-009, ADR-0001, ADR-0002
@RestController
class WebhookController {

  private final UpdateHandler updates;

  WebhookController(UpdateHandler updates) {
    this.updates = updates;
  }

  @PostMapping(TelegramAuthentication.WEBHOOK)
  void update(@RequestBody JsonNode update) {
    updates.handle(update);
  }
}
