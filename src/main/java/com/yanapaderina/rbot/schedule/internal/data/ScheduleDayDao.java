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

// MVP-02, RBOT-FEAT-004, REQ-DATA-ACCESS-002
@Repository
public class ScheduleDayDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  ScheduleDayDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("schedule-day");
  }

  public List<DayRow> between(LocalDate from, LocalDate to) {
    return jdbc.sql(sql.get("between")).param("from", from).param("to", to).query(Rows.DAY).list();
  }

  public boolean upsert(LocalDate day, boolean closed, String note) {
    return jdbc.sql(sql.get("upsert")).param("day", day).param("closed", closed).param("note", note).update() == 1;
  }

  public int delete(LocalDate day) {
    return jdbc.sql(sql.get("delete")).param("day", day).update();
  }

  public int deleteIntervals(LocalDate day) {
    return jdbc.sql(sql.get("delete-intervals")).param("day", day).update();
  }

  public boolean insertInterval(UUID id, LocalDate day, LocalTime starts, LocalTime ends, List<UUID> types) {
    return jdbc.sql(sql.get("insert-interval")).param("id", id).param("day", day).param("starts", starts).param("ends", ends)
      .param("types", types.stream().map(UUID::toString).collect(Collectors.joining(","))).update() == 1;
  }
}
