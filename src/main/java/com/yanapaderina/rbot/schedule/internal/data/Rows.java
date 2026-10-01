package com.yanapaderina.rbot.schedule.internal.data;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

// MVP-02, RBOT-FEAT-004, RBOT-FEAT-016, REQ-PERSISTENCE-013
final class Rows {

  static final RowMapper<SettingsRow> SETTINGS = (rs, n) -> new SettingsRow(rs.getString("zone"),
    integer(rs, "lead_minutes"), integer(rs, "horizon_days"), integer(rs, "slot_step_minutes"));

  static final RowMapper<IntervalRow> INTERVAL = (rs, n) -> new IntervalRow(rs.getInt("weekday"),
    rs.getObject("starts", LocalTime.class), rs.getObject("ends", LocalTime.class), types(rs));

  static final RowMapper<DayRow> DAY = (rs, n) -> new DayRow(rs.getObject("day", java.time.LocalDate.class),
    rs.getBoolean("closed"), rs.getString("note"), rs.getObject("starts", LocalTime.class), rs.getObject("ends", LocalTime.class),
    types(rs));

  static final RowMapper<SessionTypeRow> SESSION_TYPE = (rs, n) -> new SessionTypeRow(rs.getObject("id", UUID.class),
    rs.getString("title"), rs.getInt("duration_minutes"), rs.getBigDecimal("price"), rs.getInt("buffer_minutes"), rs.getString("format"),
    rs.getBoolean("active"));

  private Rows() {
  }

  private static List<UUID> types(ResultSet rs) throws SQLException {
    Array array = rs.getArray("session_types");
    return array == null ? List.of() : List.of((UUID[]) array.getArray());
  }

  private static Integer integer(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }
}
