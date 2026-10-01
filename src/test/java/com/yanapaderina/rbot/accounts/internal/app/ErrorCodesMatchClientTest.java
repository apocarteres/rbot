package com.yanapaderina.rbot.accounts.internal.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.yanapaderina.rbot.booking.internal.app.BookingRefused;
import com.yanapaderina.rbot.clients.ClientRefused;
import com.yanapaderina.rbot.schedule.internal.app.ScheduleRefused;
import io.github.apocarteres.platform.auth.AuthRefused;
import io.github.apocarteres.platform.web.errors.ErrorCode;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// RBOT-API-001, MVP-02, RBOT-FEAT-002, REQ-API-001
class ErrorCodesMatchClientTest {

  private static final Path CLIENT_TEXTS = Path.of("frontend/shared/failures.ts");
  private static final Pattern KEY = Pattern.compile("^\\s*'([a-z]+(?:-[a-z]+)*)':", Pattern.MULTILINE);
  private static final Set<ErrorCode> CORE_CODES_SHOWN = Set.of(AuthRefused.CREDENTIALS, AuthRefused.BLOCKED, AuthRefused.UNVERIFIED,
    AuthRefused.ENTRY, AuthRefused.EMAIL, AuthRefused.PASSWORD, AuthRefused.EMAIL_TAKEN);

  @Test
  @DisplayName("Каждый код ошибки службы и показываемый код ядра есть в словаре текстов клиента")
  void everyServerCodeHasClientText() throws IOException {
    Set<String> server = Stream.of(declared(AccountRefused.class), declared(ScheduleRefused.class), declared(BookingRefused.class), declared(ClientRefused.class), CORE_CODES_SHOWN.stream()).flatMap(codes -> codes)
      .map(ErrorCode::value)
      .collect(Collectors.toSet());
    assertThat(clientCodes()).containsAll(server);
  }

  @Test
  @DisplayName("Коды отказов разбора запроса есть в словаре клиента")
  void requestCodesHaveClientText() throws IOException {
    assertThat(clientCodes()).contains("request-unreadable", "parameter-rejected");
  }

  private static Stream<ErrorCode> declared(Class<?> owner) {
    return Arrays.stream(owner.getDeclaredFields())
      .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == ErrorCode.class)
      .map(ErrorCodesMatchClientTest::value);
  }

  private static ErrorCode value(Field field) {
    try {
      return (ErrorCode) field.get(null);
    } catch (IllegalAccessException refused) {
      throw new IllegalStateException(refused);
    }
  }

  private static Set<String> clientCodes() throws IOException {
    return KEY.matcher(Files.readString(CLIENT_TEXTS, StandardCharsets.UTF_8)).results()
      .map(match -> match.group(1))
      .collect(Collectors.toSet());
  }
}
