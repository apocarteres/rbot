package com.yanapaderina.rbot.policy.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// RBOT-FEAT-026, REQ-DATA-ACCESS-002
@Repository
public class CancellationPolicyDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  CancellationPolicyDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("cancellation-policy");
  }

  public boolean claim(UUID practitioner, Instant at) {
    return jdbc.sql(sql.get("claim")).param("practitioner", practitioner).param("createdAt", StoredInstant.offsetOf(at)).update() == 1;
  }

  public boolean exists(UUID practitioner) {
    return jdbc.sql(sql.get("exists")).param("practitioner", practitioner).query(Long.class).single() > 0;
  }
}
