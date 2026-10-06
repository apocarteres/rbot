package com.yanapaderina.rbot.bell.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// RBOT-FEAT-020, REQ-DATA-ACCESS-002
@Repository
public class BellPreferenceDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  BellPreferenceDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("bell-preference");
  }

  public Optional<Boolean> sound(UUID account) {
    return jdbc.sql(sql.get("find")).param("account", account).query(Boolean.class).optional();
  }

  public boolean upsert(UUID account, boolean sound, Instant at) {
    return jdbc.sql(sql.get("upsert")).param("account", account).param("sound", sound).param("updatedAt", StoredInstant.offsetOf(at))
      .update() == 1;
  }
}
