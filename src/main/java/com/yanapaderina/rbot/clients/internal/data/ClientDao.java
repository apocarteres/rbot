package com.yanapaderina.rbot.clients.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-03, RBOT-FEAT-002, REQ-DATA-ACCESS-002
@Repository
public class ClientDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  ClientDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("client");
  }

  public Optional<UUID> findByAccount(UUID accountId) {
    return jdbc.sql(sql.get("find-by-account")).param("accountId", accountId).query(UUID.class).optional();
  }

  public boolean insertForAccount(UUID id, UUID accountId, Instant at) {
    return jdbc.sql(sql.get("insert-for-account")).param("id", id).param("accountId", accountId)
      .param("createdAt", StoredInstant.offsetOf(at)).update() == 1;
  }
}
