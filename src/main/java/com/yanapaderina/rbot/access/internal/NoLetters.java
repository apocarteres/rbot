package com.yanapaderina.rbot.access.internal;

import io.github.apocarteres.platform.auth.AuthLetters;
import java.net.URI;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

// MVP-01, REQ-AUTH-010, REQ-AUTH-013
@Component
class NoLetters implements AuthLetters {

  private static final Log LOG = LogFactory.getLog(NoLetters.class);

  @Override
  public void verification(String email, URI link, Locale locale) {
    skipped("verification");
  }

  @Override
  public void passwordReset(String email, URI link, Locale locale) {
    skipped("password-reset");
  }

  @Override
  public void emailChange(String email, URI link, Locale locale) {
    skipped("email-change");
  }

  @Override
  public void emailChanged(String previousEmail, Locale locale) {
    skipped("email-changed");
  }

  private static void skipped(String letter) {
    LOG.warn("Письмо " + letter + " не отправлено: почта не подключена");
  }
}
