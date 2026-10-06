package com.yanapaderina.rbot.schedule.internal.app;

import io.github.apocarteres.platform.web.errors.CodedFailure;
import io.github.apocarteres.platform.web.errors.ErrorCode;
import org.springframework.http.HttpStatus;

// MVP-02, RBOT-FEAT-004, RBOT-FEAT-008, REQ-API-011, RBOT-FEAT-021
public final class ScheduleRefused extends RuntimeException implements CodedFailure {

  private static final long serialVersionUID = 1L;

  public static final ErrorCode INTERVAL = ErrorCode.of("interval-rejected", HttpStatus.BAD_REQUEST);
  public static final ErrorCode INTERVAL_TYPES_REQUIRED = ErrorCode.of("interval-types-required", HttpStatus.BAD_REQUEST);
  public static final ErrorCode INTERVAL_TYPE = ErrorCode.of("interval-type-unknown", HttpStatus.BAD_REQUEST);
  public static final ErrorCode OVERLAP = ErrorCode.of("intervals-overlap", HttpStatus.BAD_REQUEST);
  public static final ErrorCode RANGE = ErrorCode.of("range-rejected", HttpStatus.BAD_REQUEST);
  public static final ErrorCode SETTINGS = ErrorCode.of("settings-rejected", HttpStatus.BAD_REQUEST);
  public static final ErrorCode INCOMPLETE = ErrorCode.of("settings-incomplete", HttpStatus.CONFLICT);
  public static final ErrorCode TYPE = ErrorCode.of("session-type-rejected", HttpStatus.BAD_REQUEST);
  public static final ErrorCode OPENING = ErrorCode.of("opening-rejected", HttpStatus.BAD_REQUEST);
  public static final ErrorCode TYPE_MISSING = ErrorCode.of("session-type-missing", HttpStatus.NOT_FOUND);

  private final transient ErrorCode code;

  public ScheduleRefused(ErrorCode code, String reason) {
    super(reason, null, false, false);
    this.code = code;
  }

  @Override
  public ErrorCode code() {
    return code;
  }
}
