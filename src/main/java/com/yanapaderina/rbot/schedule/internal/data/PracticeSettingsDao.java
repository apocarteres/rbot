package com.yanapaderina.rbot.schedule.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-02, RBOT-FEAT-016, REQ-DATA-ACCESS-002
@Repository
public class PracticeSettingsDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  PracticeSettingsDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("practice-settings");
  }

  public SettingsRow find() {
    return jdbc.sql(sql.get("find")).query(Rows.SETTINGS).single();
  }

  public boolean update(SettingsRow row, Instant at) {
    return jdbc.sql(sql.get("update"))
      .param("zone", row.zone())
      .param("leadMinutes", row.leadMinutes())
      .param("horizonDays", row.horizonDays())
      .param("slotStepMinutes", row.slotStepMinutes())
      .param("updatedAt", OffsetDateTime.ofInstant(at, ZoneOffset.UTC))
      .update() == 1;
  }
}
