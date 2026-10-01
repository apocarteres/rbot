package com.yanapaderina.rbot.web.internal;

import io.github.apocarteres.platform.web.errors.ErrorCode;
import io.github.apocarteres.platform.web.errors.ErrorCodeResolver;
import java.util.Optional;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

// MVP-02, REQ-API-002
@Component
class RequestErrorCodes implements ErrorCodeResolver {

  static final ErrorCode UNREADABLE = ErrorCode.of("request-unreadable", HttpStatus.BAD_REQUEST);
  static final ErrorCode PARAMETER = ErrorCode.of("parameter-rejected", HttpStatus.BAD_REQUEST);

  @Override
  public Optional<ErrorCode> resolve(Throwable failure) {
    if (failure instanceof HttpMessageNotReadableException) {
      return Optional.of(UNREADABLE);
    }
    if (failure instanceof TypeMismatchException) {
      return Optional.of(PARAMETER);
    }
    return Optional.empty();
  }
}
