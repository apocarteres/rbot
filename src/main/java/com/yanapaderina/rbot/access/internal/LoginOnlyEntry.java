package com.yanapaderina.rbot.access;

import io.github.apocarteres.platform.auth.EntryAccess;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

// MVP-01, REQ-AUTH-016
@Component
class LoginOnlyEntry implements EntryAccess {

  static final String LOGIN = "/api/auth/login";

  @Override
  public boolean allowed(HttpServletRequest request) {
    return LOGIN.equals(request.getRequestURI());
  }
}
