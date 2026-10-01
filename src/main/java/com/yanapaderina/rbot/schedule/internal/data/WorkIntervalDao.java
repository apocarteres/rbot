package com.yanapaderina.rbot.schedule.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-02, RBOT-FEAT-004, RBOT-FEAT-016, RBOT-FEAT-017, REQ-DATA-ACCESS-002
@Repository
public class WorkIntervalDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  WorkIntervalDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("work-interval");
  }

  public List<IntervalRow> list(UUID practitioner) {
    return jdbc.sql(sql.get("list")).param("practitioner", practitioner).query(Rows.INTERVAL).list();
  }

  public int deleteWeekday(UUID practitioner, int weekday) {
    return jdbc.sql(sql.get("delete-weekday")).param("practitioner", practitioner).param("weekday", weekday).update();
  }

  public boolean insert(UUID id, UUID practitioner, int weekday, LocalTime starts, LocalTime ends, List<UUID> types) {
    return jdbc.sql(sql.get("insert")).param("id", id).param("practitioner", practitioner).param("weekday", weekday)
      .param("starts", starts).param("ends", ends)
      .param("types", types.stream().map(UUID::toString).collect(Collectors.joining(","))).update() == 1;
  }

  public int dropType(UUID practitioner, UUID type) {
    return jdbc.sql(sql.get("drop-type")).param("practitioner", practitioner).param("type", type).update();
  }

  public int deleteEmpty(UUID practitioner) {
    return jdbc.sql(sql.get("delete-empty")).param("practitioner", practitioner).update();
  }
}
