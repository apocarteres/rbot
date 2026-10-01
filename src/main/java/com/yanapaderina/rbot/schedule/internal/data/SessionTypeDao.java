package com.yanapaderina.rbot.schedule.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-02, REQ-DATA-ACCESS-002
@Repository
public class SessionTypeDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  SessionTypeDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("session-type");
  }

  public List<SessionTypeRow> list() {
    return jdbc.sql(sql.get("list")).query(Rows.SESSION_TYPE).list();
  }

  public Optional<SessionTypeRow> find(UUID id) {
    return jdbc.sql(sql.get("find")).param("id", id).query(Rows.SESSION_TYPE).optional();
  }

  public boolean insert(SessionTypeRow row, Instant at) {
    return params(jdbc.sql(sql.get("insert")), row).param("createdAt", OffsetDateTime.ofInstant(at, ZoneOffset.UTC)).update() == 1;
  }

  public boolean update(SessionTypeRow row) {
    return params(jdbc.sql(sql.get("update")), row).update() == 1;
  }

  private static JdbcClient.StatementSpec params(JdbcClient.StatementSpec statement, SessionTypeRow row) {
    return statement.param("id", row.id()).param("title", row.title()).param("durationMinutes", row.durationMinutes())
      .param("price", row.price()).param("format", row.format()).param("firstVisit", row.firstVisit()).param("active", row.active());
  }
}
