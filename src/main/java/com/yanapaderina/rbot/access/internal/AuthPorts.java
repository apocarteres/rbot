package com.yanapaderina.rbot.access;

import io.github.apocarteres.platform.auth.HumanCheck;
import io.github.apocarteres.platform.auth.NoProfile;
import io.github.apocarteres.platform.auth.RegistrationHook;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// MVP-01, REQ-AUTH-003, REQ-AUTH-021
@Configuration(proxyBeanMethods = false)
class AuthPorts {

  @Bean
  HumanCheck humanCheck() {
    return HumanCheck.NOT_REQUIRED;
  }

  @Bean
  RegistrationHook<NoProfile> registrationHook() {
    return RegistrationHook.NONE;
  }
}
