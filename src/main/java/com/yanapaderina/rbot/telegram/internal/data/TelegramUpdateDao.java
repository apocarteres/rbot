package com.yanapaderina.rbot.telegram.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-04, RBOT-FEAT-009, REQ-DATA-ACCESS-002
@Repository
public class TelegramUpdateDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  TelegramUpdateDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("telegram-update");
  }

  public boolean remember(long updateId, Instant at) {
    return jdbc.sql(sql.get("remember")).param("updateId", updateId).param("at", StoredInstant.offsetOf(at)).update() == 1;
  }
}
