package com.yanapaderina.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

// RBOT-FEAT-017, ADR-0003, REQ-DATA-ACCESS-008
public final class PracticeOwner implements CustomTaskChange {

  static final String OWNER = "RBOT_PRACTICE_OWNER";
  static final List<String> TABLES = List.of("practice_settings", "work_interval", "schedule_day", "schedule_day_interval",
    "session_type", "client", "session");
  private static final String BY_EMAIL = "SELECT id FROM platform_account WHERE lower(email) = lower(?)";
  private static final String EARLIEST = "SELECT a.id FROM platform_account a WHERE EXISTS (SELECT 1 FROM platform_account_role r "
    + "WHERE r.account_id = a.id AND r.role = 'PSYCHOLOGIST') AND NOT EXISTS (SELECT 1 FROM platform_account_role r "
    + "WHERE r.account_id = a.id AND r.role = 'ADMIN') ORDER BY a.created_at LIMIT 1";
  private static final String ANY = "SELECT a.id FROM platform_account a WHERE EXISTS (SELECT 1 FROM platform_account_role r "
    + "WHERE r.account_id = a.id AND r.role = 'PSYCHOLOGIST') ORDER BY a.created_at LIMIT 1";

  @Override
  public void execute(Database database) throws CustomChangeException {
    Connection connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
    try {
      Optional<Object> owner = owner(connection, System.getenv(OWNER));
      for (String table : TABLES) {
        if (owner.isPresent()) {
          try (PreparedStatement update = connection.prepareStatement("UPDATE " + table + " SET practitioner_id = ? WHERE practitioner_id IS NULL")) {
            update.setObject(1, owner.get());
            update.executeUpdate();
          }
        }
      }
      if (owner.isEmpty()) {
        for (String table : List.of("session", "schedule_day_interval", "schedule_day", "work_interval", "client", "session_type")) {
          try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE practitioner_id IS NULL")) {
            delete.executeUpdate();
          }
        }
      }
    } catch (SQLException failed) {
      throw new CustomChangeException("Практика не назначена: " + failed.getMessage(), failed);
    }
  }

  static Optional<Object> owner(Connection connection, String email) throws SQLException {
    if (email != null && !email.isBlank()) {
      Optional<Object> named = first(connection, BY_EMAIL, email.trim());
      if (named.isEmpty()) {
        throw new SQLException("Учётной записи " + OWNER + " нет");
      }
      return named;
    }
    Optional<Object> earliest = first(connection, EARLIEST, null);
    return earliest.isPresent() ? earliest : first(connection, ANY, null);
  }

  private static Optional<Object> first(Connection connection, String sql, String argument) throws SQLException {
    try (PreparedStatement query = connection.prepareStatement(sql)) {
      if (argument != null) {
        query.setString(1, argument);
      }
      try (ResultSet result = query.executeQuery()) {
        return result.next() ? Optional.of(result.getObject(1)) : Optional.empty();
      }
    }
  }

  @Override
  public String getConfirmationMessage() {
    return "Данные практики назначены психологу";
  }

  @Override
  public void setUp() {
  }

  @Override
  public void setFileOpener(ResourceAccessor accessor) {
  }

  @Override
  public ValidationErrors validate(Database database) {
    return new ValidationErrors();
  }
}
