package com.yanapaderina.rbot.schedule.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// RBOT-FEAT-021, REQ-DATA-ACCESS-002
@Repository
public class OpenSlotDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  OpenSlotDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("open-slot");
  }

  public List<Instant> between(UUID practitioner, Instant from, Instant to) {
    return jdbc.sql(sql.get("between")).param("practitioner", practitioner).param("from", StoredInstant.offsetOf(from))
      .param("to", StoredInstant.offsetOf(to)).query((rs, n) -> rs.getObject("start_at", OffsetDateTime.class).toInstant()).list();
  }

  public boolean open(UUID practitioner, Instant start, Instant at) {
    return jdbc.sql(sql.get("open")).param("practitioner", practitioner).param("start", StoredInstant.offsetOf(start))
      .param("openedAt", StoredInstant.offsetOf(at)).update() == 1;
  }

  public boolean close(UUID practitioner, Instant start) {
    return jdbc.sql(sql.get("close")).param("practitioner", practitioner).param("start", StoredInstant.offsetOf(start)).update() == 1;
  }
}
