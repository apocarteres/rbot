package com.yanapaderina.rbot.accounts;

import io.github.apocarteres.platform.auth.Account;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// MVP-01, REQ-CODE-DESIGN-005
record AccountView(UUID id, String email, List<String> roles, boolean blocked, Instant createdAt, Instant lastLoginAt) {

  static AccountView of(Account account) {
    return new AccountView(account.id(), account.email(), account.roles().stream().sorted().toList(), account.blocked(),
      account.createdAt(), account.lastLoginAt());
  }
}
