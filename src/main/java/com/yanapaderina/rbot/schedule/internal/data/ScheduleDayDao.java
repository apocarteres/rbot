package com.yanapaderina.rbot.schedule.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-02, RBOT-FEAT-004, RBOT-FEAT-016, RBOT-FEAT-017, REQ-DATA-ACCESS-002
@Repository
public class ScheduleDayDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  ScheduleDayDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("schedule-day");
  }

  public List<DayRow> between(UUID practitioner, LocalDate from, LocalDate to) {
    return jdbc.sql(sql.get("between")).param("practitioner", practitioner).param("from", from).param("to", to).query(Rows.DAY).list();
  }

  public boolean upsert(UUID practitioner, LocalDate day, boolean closed, String note) {
    return jdbc.sql(sql.get("upsert")).param("practitioner", practitioner).param("day", day).param("closed", closed).param("note", note)
      .update() == 1;
  }

  public int delete(UUID practitioner, LocalDate day) {
    return jdbc.sql(sql.get("delete")).param("practitioner", practitioner).param("day", day).update();
  }

  public int deleteIntervals(UUID practitioner, LocalDate day) {
    return jdbc.sql(sql.get("delete-intervals")).param("practitioner", practitioner).param("day", day).update();
  }

  public boolean insertInterval(UUID id, UUID practitioner, LocalDate day, LocalTime starts, LocalTime ends, List<UUID> types) {
    return jdbc.sql(sql.get("insert-interval")).param("id", id).param("practitioner", practitioner).param("day", day)
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
