package com.yanapaderina.rbot.accounts.internal.app;

import io.github.apocarteres.platform.auth.Accounts;
import io.github.apocarteres.platform.auth.NoProfile;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// MVP-01, RUN-QA, REQ-AUTH-009
@Configuration(proxyBeanMethods = false)
@Profile("qa & !production")
@EnableConfigurationProperties(QaAccountSeeds.class)
class QaAccounts {

  @Bean
  ApplicationRunner qaAccountSeeding(Accounts accounts, QaAccountSeeds seeds) {
    return arguments -> seeds.accounts().stream()
      .filter(seed -> accounts.findByEmail(seed.email()).isEmpty())
      .forEach(seed -> accounts.create(seed.email(), seed.password(), seed.roles(), true, new NoProfile()));
  }
}
