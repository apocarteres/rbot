package com.yanapaderina.rbot.clients.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-03, RBOT-FEAT-009, ADR-0002, REQ-DATA-ACCESS-002
@Repository
public class InviteDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  InviteDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("client-invite");
  }

  public boolean insert(UUID id, UUID clientId, String tokenHash, Instant expiresAt, Instant at) {
    return jdbc.sql(sql.get("insert")).param("id", id).param("clientId", clientId).param("tokenHash", tokenHash)
      .param("expiresAt", StoredInstant.offsetOf(expiresAt)).param("createdAt", StoredInstant.offsetOf(at)).update() == 1;
  }

  public int withdraw(UUID clientId) {
    return jdbc.sql(sql.get("withdraw")).param("clientId", clientId).update();
  }

  public Optional<UUID> open(String tokenHash, Instant now) {
    return jdbc.sql(sql.get("open")).param("tokenHash", tokenHash).param("now", StoredInstant.offsetOf(now)).query(UUID.class)
      .optional();
  }

  public Optional<UUID> redeem(String tokenHash, Instant now) {
    return jdbc.sql(sql.get("redeem")).param("tokenHash", tokenHash).param("now", StoredInstant.offsetOf(now)).query(UUID.class)
      .optional();
  }
}
