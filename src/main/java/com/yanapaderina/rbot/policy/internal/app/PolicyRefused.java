package com.yanapaderina.rbot.policy.internal.app;

import io.github.apocarteres.platform.web.errors.CodedFailure;
import io.github.apocarteres.platform.web.errors.ErrorCode;
import org.springframework.http.HttpStatus;

// RBOT-FEAT-026, REQ-API-011
public final class PolicyRefused extends RuntimeException implements CodedFailure {

  private static final long serialVersionUID = 1L;

  public static final ErrorCode RULE = ErrorCode.of("rule-rejected", HttpStatus.BAD_REQUEST);
  public static final ErrorCode HOURS_TAKEN = ErrorCode.of("rule-hours-taken", HttpStatus.CONFLICT);
  public static final ErrorCode MISSING = ErrorCode.of("rule-missing", HttpStatus.NOT_FOUND);

  private final transient ErrorCode code;

  public PolicyRefused(ErrorCode code, String reason) {
    super(reason, null, false, false);
    this.code = code;
  }

  @Override
  public ErrorCode code() {
    return code;
  }
}
