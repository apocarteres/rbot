package com.yanapaderina.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

// RBOT-OPS-011, RBOT-FEAT-002, RBOT-FEAT-004, RBOT-FEAT-005, RBOT-FEAT-016, RBOT-FEAT-017, RBOT-FEAT-026, REQ-DATA-ACCESS-008
class SchemaMigrationIT {

  private static final List<String> APPLIED = List.of("platform-auth:001-account", "platform-auth:002-role", "platform-auth:003-token",
    "platform-auth:004-access-key", "platform-job-lock:001-lock", "platform-notifications:001-notification", "rbot:001-schedule", "rbot:002-clients", "rbot:003-booking", "rbot:004-interval-types", "rbot:005-session-reschedule",
    "rbot:006-interval-types-required", "rbot:007-client-invites", "rbot:008-telegram-updates", "rbot:009-session-type-buffer", "rbot:010-practices", "rbot:011-consent-texts", "rbot:012-telegram-polling-lease", "rbot:013-open-slots", "rbot:014-bell-preferences", "rbot:015-cancellation-rules");

  @Test
  void freshDatabaseGetsCoreChangelog() throws Exception {
    try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
      postgres.start();
      DriverManagerDataSource source = source(postgres);
      new SchemaMigration(source).migrate();
      assertThat(applied(source)).isEqualTo(APPLIED);
      assertThat(new JdbcTemplate(source).queryForObject("SELECT count(*) FROM session_type", Integer.class)).isZero();
    }
  }

  @Test
  void flywayDatabaseIsAdoptedWithoutLosingAccounts() throws Exception {
    try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
      postgres.start();
      DriverManagerDataSource source = source(postgres);
      JdbcTemplate jdbc = new JdbcTemplate(source);
      jdbc.execute(new ClassPathResource("legacy/flyway-v1.sql").getContentAsString(StandardCharsets.UTF_8));
      jdbc.execute("CREATE TABLE flyway_schema_history (installed_rank INT PRIMARY KEY, version VARCHAR(50))");
      jdbc.update("INSERT INTO platform_account (id, email, password_hash, email_verified, created_at) "
        + "VALUES ('00000000-0000-0000-0000-000000000001', 'admin@example.test', 'hash', TRUE, now())");

      new SchemaMigration(source).migrate();
      new SchemaMigration(source).migrate();

      assertThat(applied(source)).isEqualTo(APPLIED);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM platform_account", Integer.class)).isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT to_regclass('public.platform_access_key') IS NOT NULL", Boolean.class)).isTrue();
    }
  }

  private static DriverManagerDataSource source(PostgreSQLContainer postgres) {
    return new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
  }

  private static List<String> applied(DriverManagerDataSource source) {
    return new JdbcTemplate(source).queryForList("SELECT id FROM databasechangelog ORDER BY orderexecuted", String.class);
  }
}
