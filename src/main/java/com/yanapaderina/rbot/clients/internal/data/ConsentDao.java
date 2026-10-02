package com.yanapaderina.rbot.clients.internal.data;

import com.yanapaderina.rbot.clients.ConsentText;
import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// MVP-03, RBOT-FEAT-009, RBOT-FEAT-018, ADR-0005, REQ-DATA-ACCESS-002
@Repository
public class ConsentDao {

  private final JdbcClient jdbc;
  private final SqlCatalog consents;
  private final SqlCatalog texts;

  ConsentDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.consents = statements.catalog("consent");
    this.texts = statements.catalog("consent-text");
  }

  public boolean insert(UUID id, UUID clientId, int version, String channel, Instant at) {
    return jdbc.sql(consents.get("insert")).param("id", id).param("clientId", clientId).param("version", version)
      .param("channel", channel).param("acceptedAt", StoredInstant.offsetOf(at)).update() == 1;
  }

  public List<UUID> due(long telegramUserId) {
    return jdbc.sql(consents.get("due")).param("telegramUserId", telegramUserId).query(UUID.class).list();
  }

  public Optional<ConsentText> latestText(UUID practitioner) {
    return jdbc.sql(texts.get("latest")).param("practitioner", practitioner)
      .query((rs, n) -> new ConsentText(rs.getInt("version"), rs.getString("body"),
        Optional.of(rs.getObject("created_at", OffsetDateTime.class).toInstant())))
      .optional();
  }

  public boolean insertText(UUID id, UUID practitioner, int version, String body, Instant at) {
    return jdbc.sql(texts.get("insert")).param("id", id).param("practitioner", practitioner).param("version", version)
      .param("body", body).param("createdAt", StoredInstant.offsetOf(at)).update() == 1;
  }
}
