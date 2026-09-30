package com.yanapaderina.rbot;

import com.yanapaderina.migration.MigrationRun;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

// MVP-01, REQ-DEPLOYMENT-007
@SpringBootApplication
public class RbotApplication {

  static final String MIGRATE = "migrate";

  public static void main(String[] args) {
    if (args.length > 0 && MIGRATE.equals(args[0])) {
      System.exit(migrate());
    }
    SpringApplication.run(RbotApplication.class, args);
  }

  static int migrate() {
    ConfigurableApplicationContext context = new SpringApplicationBuilder(MigrationRun.class)
      .profiles(MIGRATE)
      .web(WebApplicationType.NONE)
      .run();
    return SpringApplication.exit(context);
  }
}
