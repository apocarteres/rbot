package com.yanapaderina.rbot.schedule.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-02, REQ-DATA-ACCESS-002
@Repository
public class WorkIntervalDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  WorkIntervalDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("work-interval");
  }

  public List<IntervalRow> list() {
    return jdbc.sql(sql.get("list")).query(Rows.INTERVAL).list();
  }

  public int deleteWeekday(int weekday) {
    return jdbc.sql(sql.get("delete-weekday")).param("weekday", weekday).update();
  }

  public boolean insert(UUID id, int weekday, LocalTime starts, LocalTime ends) {
    return jdbc.sql(sql.get("insert")).param("id", id).param("weekday", weekday).param("starts", starts).param("ends", ends)
      .update() == 1;
  }
}
