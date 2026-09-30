package com.yanapaderina.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.Scope;
import liquibase.command.CommandScope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringResourceAccessor;
import org.springframework.core.io.DefaultResourceLoader;

// RBOT-OPS-011, REQ-DATA-ACCESS-007, REQ-DATA-ACCESS-008, REQ-DEPLOYMENT-007
public final class SchemaMigration {

  static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";
  static final List<String> FLYWAY_V1 = List.of("platform-auth:001-account", "platform-auth:002-role", "platform-auth:003-token");

  private final DataSource source;

  public SchemaMigration(DataSource source) {
    this.source = source;
  }

  public void migrate() throws Exception {
    try (Connection connection = source.getConnection()) {
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
      Map<String, Object> scope = Map.of(Scope.Attr.resourceAccessor.name(), new SpringResourceAccessor(new DefaultResourceLoader(getClass().getClassLoader())));
      Scope.child(scope, () -> {
        if (createdByFlyway(connection)) {
          for (int i = 0; i < FLYWAY_V1.size(); i++) {
            command("markNextChangesetRan", database);
          }
          List<String> marked = applied(connection);
          if (!FLYWAY_V1.equals(marked)) {
            throw new IllegalStateException("Приняты не те наборы изменений: " + marked + ", ожидались " + FLYWAY_V1);
          }
        }
        command("update", database);
      });
    }
  }

  private static void command(String name, Database database) throws Exception {
    new CommandScope(name).addArgumentValue("database", database).addArgumentValue("changelogFile", CHANGELOG).execute();
  }

  private static boolean createdByFlyway(Connection connection) throws SQLException {
    return exists(connection, "flyway_schema_history") && exists(connection, "platform_account")
      && !exists(connection, "databasechangelog");
  }

  private static boolean exists(Connection connection, String table) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
      statement.setString(1, "public." + table);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() && result.getBoolean(1);
      }
    }
  }

  private static List<String> applied(Connection connection) throws SQLException {
    List<String> ids = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM databasechangelog ORDER BY orderexecuted");
         ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        ids.add(result.getString(1));
      }
    }
    return ids;
  }
}
