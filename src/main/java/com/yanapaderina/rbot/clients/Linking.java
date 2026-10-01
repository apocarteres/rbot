package com.yanapaderina.rbot.clients;

import java.util.UUID;

// MVP-03, RBOT-FEAT-009, ADR-0002, REQ-CODE-DESIGN-002
public sealed interface Linking {

  record Linked(UUID clientId) implements Linking {
  }

  record InviteRejected() implements Linking {
  }

  record TelegramTaken() implements Linking {
  }
}
