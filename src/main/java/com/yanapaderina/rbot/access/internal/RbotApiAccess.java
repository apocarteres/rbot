package com.yanapaderina.rbot.access.internal;

import com.yanapaderina.rbot.access.Roles;
import io.github.apocarteres.platform.auth.ApiAccess;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

// MVP-01, REQ-AUTH-014
@Component
class RbotApiAccess implements ApiAccess {

  @Override
  public void rules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules) {
    rules.requestMatchers("/api/admin/**").hasRole(Roles.ADMIN)
      .requestMatchers("/api/cabinet/**").hasAnyRole(Roles.PSYCHOLOGIST, Roles.ADMIN);
  }
}
