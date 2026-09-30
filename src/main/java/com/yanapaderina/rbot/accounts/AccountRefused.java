package com.yanapaderina.rbot.accounts;

import io.github.apocarteres.platform.web.errors.CodedFailure;
import io.github.apocarteres.platform.web.errors.ErrorCode;
import org.springframework.http.HttpStatus;

// MVP-01, REQ-API-011
final class AccountRefused extends RuntimeException implements CodedFailure {

  private static final long serialVersionUID = 1L;

  static final ErrorCode ROLES = ErrorCode.of("roles-rejected", HttpStatus.BAD_REQUEST);
  static final ErrorCode MISSING = ErrorCode.of("account-missing", HttpStatus.NOT_FOUND);
  static final ErrorCode SELF = ErrorCode.of("self-change-refused", HttpStatus.CONFLICT);

  private final transient ErrorCode code;

  AccountRefused(ErrorCode code, String reason) {
    super(reason, null, false, false);
    this.code = code;
  }

  @Override
  public ErrorCode code() {
    return code;
  }
}
