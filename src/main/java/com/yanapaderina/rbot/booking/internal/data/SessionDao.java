package com.yanapaderina.rbot.booking.internal.data;

import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-017, ADR-0003, REQ-DATA-ACCESS-002, REQ-PERSISTENCE-013
@Repository
public class SessionDao {

  private static final RowMapper<SessionRow> SESSION = (rs, n) -> new SessionRow(rs.getObject("id", UUID.class),
    rs.getObject("practitioner_id", UUID.class), rs.getObject("client_id", UUID.class), rs.getObject("type_id", UUID.class), instant(rs, "starts_at"), instant(rs, "ends_at"),
    rs.getString("status"), rs.getBigDecimal("price_snapshot"), rs.getObject("cancelled_by", UUID.class),
    rs.getObject("rescheduled_to", UUID.class));

  private static final RowMapper<OccupiedRow> OCCUPIED = (rs, n) -> new OccupiedRow(instant(rs, "starts"), instant(rs, "ends"));

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  SessionDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("session");
  }

  public boolean insert(SessionRow row, Instant occupiedUntil, UUID createdBy, Instant at) {
    return jdbc.sql(sql.get("insert")).param("id", row.id()).param("practitioner", row.practitioner()).param("clientId", row.client()).param("typeId", row.type())
      .param("startsAt", StoredInstant.offsetOf(row.start())).param("endsAt", StoredInstant.offsetOf(row.end()))
      .param("occupiedUntil", StoredInstant.offsetOf(occupiedUntil)).param("status", row.status()).param("price", row.price())
      .param("createdBy", createdBy).param("createdAt", StoredInstant.offsetOf(at)).update() == 1;
  }

  public Optional<SessionRow> find(UUID id, UUID client) {
    return jdbc.sql(sql.get("find")).param("id", id).param("clientId", client).query(SESSION).optional();
  }

  public Optional<SessionRow> find(UUID id) {
    return jdbc.sql(sql.get("find-any")).param("id", id).query(SESSION).optional();
  }

  public List<SessionRow> upcoming(Collection<UUID> clients, Instant now) {
    if (clients.isEmpty()) {
      return List.of();
    }
    return jdbc.sql(sql.get("upcoming")).param("clients", clients).param("now", StoredInstant.offsetOf(now)).query(SESSION).list();
  }

  public List<SessionRow> between(UUID practitioner, Instant from, Instant to) {
    return jdbc.sql(sql.get("between")).param("practitioner", practitioner).param("from", StoredInstant.offsetOf(from)).param("to", StoredInstant.offsetOf(to))
      .query(SESSION).list();
  }

  public List<OccupiedRow> occupied(UUID practitioner, Instant from, Instant to) {
    return jdbc.sql(sql.get("occupied")).param("practitioner", practitioner).param("from", StoredInstant.offsetOf(from)).param("to", StoredInstant.offsetOf(to))
      .query(OCCUPIED).list();
  }

  public boolean close(UUID id, String status, UUID by, Instant at) {
    return jdbc.sql(sql.get("close")).param("id", id).param("status", status).param("by", by)
      .param("at", StoredInstant.offsetOf(at)).update() == 1;
  }

  public boolean linkReschedule(UUID id, UUID to) {
    return jdbc.sql(sql.get("link-reschedule")).param("id", id).param("to", to).update() == 1;
  }

  public boolean markNoShow(UUID id, Instant now) {
    return jdbc.sql(sql.get("mark-no-show")).param("id", id).param("now", StoredInstant.offsetOf(now)).update() == 1;
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column, OffsetDateTime.class).toInstant();
  }
}
