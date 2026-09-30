package com.yanapaderina.rbot.access.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

// MVP-01, REQ-AUTH-016
class LoginOnlyEntryTest {

  private final LoginOnlyEntry entry = new LoginOnlyEntry();

  @Test
  void opensLogin() {
    assertThat(entry.allowed(new MockHttpServletRequest("POST", "/api/auth/login"))).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/auth/register", "/api/auth/verify", "/api/auth/resend", "/api/auth/password-reset/request",
    "/api/auth/password-reset/confirm", "/api/auth/email/confirm"})
  void closesEntriesThatNeedLetters(String path) {
    assertThat(entry.allowed(new MockHttpServletRequest("POST", path))).isFalse();
  }
}
