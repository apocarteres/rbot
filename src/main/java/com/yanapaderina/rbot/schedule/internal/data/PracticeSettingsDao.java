package com.yanapaderina.rbot.schedule.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-02, RBOT-FEAT-016, RBOT-FEAT-017, REQ-DATA-ACCESS-002
@Repository
public class PracticeSettingsDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  PracticeSettingsDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("practice-settings");
  }

  public Optional<SettingsRow> find(UUID practitioner) {
    return jdbc.sql(sql.get("find")).param("practitioner", practitioner).query(Rows.SETTINGS).optional();
  }

  public Map<UUID, String> names(Collection<UUID> practitioners) {
    Map<UUID, String> names = new HashMap<>();
    if (practitioners.isEmpty()) {
      return names;
    }
    jdbc.sql(sql.get("names")).param("practitioners", practitioners)
      .query((rs, n) -> names.put(rs.getObject("practitioner_id", UUID.class), rs.getString("display_name"))).list();
    return names;
  }

  public boolean update(UUID practitioner, SettingsRow row, Instant at) {
    return jdbc.sql(sql.get("update"))
      .param("practitioner", practitioner)
      .param("zone", row.zone())
      .param("leadMinutes", row.leadMinutes())
      .param("horizonDays", row.horizonDays())
      .param("slotStepMinutes", row.slotStepMinutes())
      .param("displayName", row.displayName())
      .param("updatedAt", StoredInstant.offsetOf(at))
      .update() == 1;
  }
}
