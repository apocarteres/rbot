package com.yanapaderina.rbot.booking.internal.app;

import io.github.apocarteres.platform.web.errors.CodedFailure;
import io.github.apocarteres.platform.web.errors.ErrorCode;
import org.springframework.http.HttpStatus;

// MVP-05, RBOT-FEAT-002, REQ-API-011
public final class BookingRefused extends RuntimeException implements CodedFailure {

  private static final long serialVersionUID = 1L;

  public static final ErrorCode CLOSED = ErrorCode.of("booking-closed", HttpStatus.CONFLICT);
  public static final ErrorCode TYPE_UNAVAILABLE = ErrorCode.of("session-type-unavailable", HttpStatus.NOT_FOUND);
  public static final ErrorCode SLOT_TAKEN = ErrorCode.of("slot-taken", HttpStatus.CONFLICT);
  public static final ErrorCode SESSION_MISSING = ErrorCode.of("session-missing", HttpStatus.NOT_FOUND);
  public static final ErrorCode SESSION_INACTIVE = ErrorCode.of("session-not-active", HttpStatus.CONFLICT);
  public static final ErrorCode TOO_LATE = ErrorCode.of("cancel-too-late", HttpStatus.CONFLICT);

  private final transient ErrorCode code;

  public BookingRefused(ErrorCode code, String reason) {
    super(reason, null, false, false);
    this.code = code;
  }

  @Override
  public ErrorCode code() {
    return code;
  }
}
