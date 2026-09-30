package com.yanapaderina.migration;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

// MVP-01, RBOT-OPS-011, REQ-DEPLOYMENT-007
@SpringBootConfiguration
@EnableAutoConfiguration
public class MigrationRun {

  @Bean
  ApplicationRunner schemaMigration(DataSource source) {
    return arguments -> new SchemaMigration(source).migrate();
  }
}
