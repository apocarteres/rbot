package com.yanapaderina.rbot.clients.internal.data;

import com.yanapaderina.rbot.clients.ClientCard;
import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-03, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-017, REQ-DATA-ACCESS-002
@Repository
public class ClientDao {

  private static final RowMapper<ClientCard> CARD = (rs, n) -> new ClientCard(rs.getObject("id", UUID.class),
    rs.getObject("practitioner_id", UUID.class), Optional.ofNullable(rs.getString("label")),
    Optional.ofNullable(rs.getObject("account_id", UUID.class)), rs.getBoolean("telegram"), rs.getString("status"),
    Optional.ofNullable(rs.getObject("invite_expires_at", OffsetDateTime.class)).map(OffsetDateTime::toInstant));

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  ClientDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("client");
  }

  public Optional<UUID> findByAccount(UUID practitioner, UUID accountId) {
    return jdbc.sql(sql.get("find-by-account")).param("practitioner", practitioner).param("accountId", accountId).query(UUID.class)
      .optional();
  }

  public Optional<UUID> findByTelegram(UUID practitioner, long telegramUserId) {
    return jdbc.sql(sql.get("find-by-telegram")).param("practitioner", practitioner).param("telegramUserId", telegramUserId)
      .query(UUID.class).optional();
  }

  public List<ClientCard> byAccount(UUID accountId) {
    return jdbc.sql(sql.get("by-account")).param("accountId", accountId).query(CARD).list();
  }

  public List<ClientCard> byTelegram(long telegramUserId) {
    return jdbc.sql(sql.get("by-telegram")).param("telegramUserId", telegramUserId).query(CARD).list();
  }

  public Optional<UUID> accountOf(UUID id) {
    return jdbc.sql(sql.get("account-of")).param("id", id).query(UUID.class).optional();
  }

  public Optional<Long> telegramOf(UUID id) {
    return jdbc.sql(sql.get("telegram-of")).param("id", id).query(Long.class).optional();
  }

  public List<ClientCard> list(UUID practitioner) {
    return jdbc.sql(sql.get("list")).param("practitioner", practitioner).query(CARD).list();
  }

  public Optional<ClientCard> find(UUID id) {
    return jdbc.sql(sql.get("find")).param("id", id).query(CARD).optional();
  }

  public boolean insertForAccount(UUID id, UUID practitioner, UUID accountId, Instant at) {
    return jdbc.sql(sql.get("insert-for-account")).param("id", id).param("practitioner", practitioner).param("accountId", accountId)
      .param("createdAt", StoredInstant.offsetOf(at)).update() == 1;
  }

  public boolean insertProspect(UUID id, UUID practitioner, String label, Instant at) {
    return jdbc.sql(sql.get("insert-prospect")).param("id", id).param("practitioner", practitioner).param("label", label)
      .param("createdAt", StoredInstant.offsetOf(at)).update() == 1;
  }

  public boolean linkTelegram(UUID id, long telegramUserId) {
    return jdbc.sql(sql.get("link-telegram")).param("id", id).param("telegramUserId", telegramUserId).update() == 1;
  }
}
