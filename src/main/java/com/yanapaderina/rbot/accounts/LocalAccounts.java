package com.yanapaderina.rbot.accounts;

import io.github.apocarteres.platform.auth.Accounts;
import io.github.apocarteres.platform.auth.NoProfile;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// MVP-01, RUN-LOCAL, REQ-AUTH-009
@Configuration(proxyBeanMethods = false)
@Profile("local & !production")
@EnableConfigurationProperties(LocalAccountSeeds.class)
class LocalAccounts {

  @Bean
  ApplicationRunner localAccountSeeding(Accounts accounts, LocalAccountSeeds seeds) {
    return arguments -> seeds.accounts().stream()
      .filter(seed -> accounts.findByEmail(seed.email()).isEmpty())
      .forEach(seed -> accounts.create(seed.email(), seed.password(), seed.roles(), true, new NoProfile()));
  }
}
