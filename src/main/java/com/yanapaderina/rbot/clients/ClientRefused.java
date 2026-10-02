package com.yanapaderina.rbot.clients;

import io.github.apocarteres.platform.web.errors.CodedFailure;
import io.github.apocarteres.platform.web.errors.ErrorCode;
import org.springframework.http.HttpStatus;

// MVP-03, RBOT-FEAT-009, RBOT-FEAT-018, RBOT-FEAT-019, REQ-API-011
public final class ClientRefused extends RuntimeException implements CodedFailure {

  private static final long serialVersionUID = 1L;

  public static final ErrorCode LABEL = ErrorCode.of("client-label-rejected", HttpStatus.BAD_REQUEST);
  public static final ErrorCode MISSING = ErrorCode.of("client-card-missing", HttpStatus.NOT_FOUND);
  public static final ErrorCode NOT_INVITABLE = ErrorCode.of("client-not-invitable", HttpStatus.CONFLICT);
  public static final ErrorCode NAMELESS = ErrorCode.of("practice-name-missing", HttpStatus.CONFLICT);
  public static final ErrorCode INVITE_REJECTED = ErrorCode.of("invite-rejected", HttpStatus.NOT_FOUND);
  public static final ErrorCode TELEGRAM_TAKEN = ErrorCode.of("telegram-taken", HttpStatus.CONFLICT);
  public static final ErrorCode CONSENT_OUTDATED = ErrorCode.of("consent-outdated", HttpStatus.CONFLICT);
  public static final ErrorCode CONSENT_TEXT = ErrorCode.of("consent-text-rejected", HttpStatus.BAD_REQUEST);
  public static final ErrorCode CONSENT_TEXT_CHANGED = ErrorCode.of("consent-text-conflict", HttpStatus.CONFLICT);

  private final transient ErrorCode code;

  public ClientRefused(ErrorCode code, String reason) {
    super(reason, null, false, false);
    this.code = code;
  }

  @Override
  public ErrorCode code() {
    return code;
  }
}
