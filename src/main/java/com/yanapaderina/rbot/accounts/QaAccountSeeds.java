package com.yanapaderina.rbot.accounts;

import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

// MVP-01, RUN-QA
@ConfigurationProperties("rbot.qa")
record QaAccountSeeds(List<Seed> accounts) {

  QaAccountSeeds {
    accounts = accounts == null ? List.of() : List.copyOf(accounts);
  }

  record Seed(String email, String password, Set<String> roles) {
  }
}
