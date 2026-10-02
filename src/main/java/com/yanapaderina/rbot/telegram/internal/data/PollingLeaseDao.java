package com.yanapaderina.rbot.telegram.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// RBOT-OPS-018, RBOT-FEAT-010, REQ-DATA-ACCESS-002, REQ-DEPLOYMENT-028
@Repository
public class PollingLeaseDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  PollingLeaseDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("telegram-polling-lease");
  }

  public boolean claim(String instance, Instant startedAt, Instant now, Instant stale) {
    return jdbc.sql(sql.get("claim")).param("instance", instance).param("startedAt", StoredInstant.offsetOf(startedAt))
      .param("now", StoredInstant.offsetOf(now)).param("stale", StoredInstant.offsetOf(stale)).update() == 1;
  }

  public boolean release(String instance) {
    return jdbc.sql(sql.get("release")).param("instance", instance).param("released", StoredInstant.offsetOf(Instant.EPOCH))
      .update() == 1;
  }
}
