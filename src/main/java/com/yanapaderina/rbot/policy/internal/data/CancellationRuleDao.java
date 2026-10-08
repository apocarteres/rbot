package com.yanapaderina.rbot.policy.internal.data;

import com.yanapaderina.rbot.policy.CancellationRule;
import io.github.apocarteres.platform.persistence.SqlCatalog;
import io.github.apocarteres.platform.persistence.SqlStatements;
import io.github.apocarteres.platform.persistence.StoredInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// RBOT-FEAT-026, REQ-DATA-ACCESS-002
@Repository
public class CancellationRuleDao {

  private final JdbcClient jdbc;
  private final SqlCatalog sql;

  CancellationRuleDao(JdbcClient jdbc, SqlStatements statements) {
    this.jdbc = jdbc;
    this.sql = statements.catalog("cancellation-rule");
  }

  public List<CancellationRule> list(UUID practitioner) {
    return jdbc.sql(sql.get("list")).param("practitioner", practitioner).query(CancellationRuleDao::rule).list();
  }

  public Optional<CancellationRule> find(UUID practitioner, UUID id) {
    return jdbc.sql(sql.get("find")).param("practitioner", practitioner).param("id", id).query(CancellationRuleDao::rule).optional();
  }

  public boolean insert(UUID practitioner, CancellationRule rule, Instant at) {
    return bind(jdbc.sql(sql.get("insert")), practitioner, rule, at).update() == 1;
  }

  public boolean update(UUID practitioner, CancellationRule rule, Instant at) {
    return bind(jdbc.sql(sql.get("update")), practitioner, rule, at).update() == 1;
  }

  public boolean delete(UUID practitioner, UUID id) {
    return jdbc.sql(sql.get("delete")).param("practitioner", practitioner).param("id", id).update() == 1;
  }

  private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, UUID practitioner, CancellationRule rule, Instant at) {
    return spec.param("id", rule.id()).param("practitioner", practitioner).param("minHours", rule.hours())
      .param("allowed", rule.allowed()).param("clientText", rule.text()).param("updatedAt", StoredInstant.offsetOf(at));
  }

  private static CancellationRule rule(ResultSet rs, int row) throws SQLException {
    return new CancellationRule(rs.getObject("id", UUID.class), rs.getInt("min_hours"), rs.getBoolean("allowed"),
      rs.getString("client_text"));
  }
}
