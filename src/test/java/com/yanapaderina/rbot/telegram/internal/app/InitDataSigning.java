package com.yanapaderina.rbot.telegram.internal.app;

import java.time.Instant;

// MVP-08, RBOT-FEAT-009
public final class InitDataSigning {

  private InitDataSigning() {
  }

  public static String signed(String token, long user, Instant authDate) {
    return InitDataTest.signed(token, user, authDate);
  }
}
