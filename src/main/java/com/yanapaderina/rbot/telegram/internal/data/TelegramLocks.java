package com.yanapaderina.rbot.telegram.internal.data;

import io.github.apocarteres.platform.persistence.JdbcJobLock;
import io.github.apocarteres.platform.persistence.JobLock;
import io.github.apocarteres.platform.persistence.SqlStatements;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

// MVP-04, RBOT-FEAT-009, REQ-DEPLOYMENT-028
@Configuration(proxyBeanMethods = false)
class TelegramLocks {

  @Bean
  JobLock jobLock(JdbcClient jdbc, SqlStatements statements, Clock clock) {
    return new JdbcJobLock(jdbc, statements, clock, "rbot-" + UUID.randomUUID());
  }
}
