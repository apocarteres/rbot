package com.yanapaderina.rbot.schedule.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-02, RBOT-FEAT-016, RBOT-FEAT-017, REQ-DATA-ACCESS-002
@Repository
public class SessionTypeDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  SessionTypeDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("session-type");
  }

  public List<SessionTypeRow> list(UUID practitioner) {
    return jdbc.sql(sql.get("list")).param("practitioner", practitioner).query(Rows.SESSION_TYPE).list();
  }

  public List<SessionTypeRow> listAll(UUID practitioner) {
    return jdbc.sql(sql.get("list-all")).param("practitioner", practitioner).query(Rows.SESSION_TYPE).list();
  }

  public Optional<SessionTypeRow> find(UUID practitioner, UUID id) {
    return jdbc.sql(sql.get("find")).param("practitioner", practitioner).param("id", id).query(Rows.SESSION_TYPE).optional();
  }

  public boolean insert(UUID practitioner, SessionTypeRow row, Instant at) {
    return params(jdbc.sql(sql.get("insert")), practitioner, row).param("createdAt", StoredInstant.offsetOf(at)).update() == 1;
  }

  public boolean update(UUID practitioner, SessionTypeRow row) {
    return params(jdbc.sql(sql.get("update")), practitioner, row).update() == 1;
  }

  public boolean delete(UUID practitioner, UUID id, Instant at) {
    return jdbc.sql(sql.get("delete")).param("practitioner", practitioner).param("id", id).param("at", StoredInstant.offsetOf(at))
      .update() == 1;
  }

  private static JdbcClient.StatementSpec params(JdbcClient.StatementSpec statement, UUID practitioner, SessionTypeRow row) {
    return statement.param("practitioner", practitioner).param("id", row.id()).param("title", row.title())
      .param("durationMinutes", row.durationMinutes()).param("price", row.price()).param("bufferMinutes", row.bufferMinutes())
      .param("format", row.format()).param("active", row.active());
  }
}
