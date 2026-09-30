package com.yanapaderina.migration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.DispatcherServlet;

// RBOT-QUAL-003, REQ-QUALITY-012, REQ-DEPLOYMENT-007
@SpringBootTest(classes = MigrationRun.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
  properties = "spring.main.web-application-type=none")
@ActiveProfiles("migrate")
class MigrateWiringTest {

  @MockitoBean
  private DataSource dataSource;

  @MockitoBean(name = "schemaMigration")
  private ApplicationRunner schemaMigration;

  @Autowired
  private ApplicationContext context;

  @Test
  void migrateRuntimeRaisesItsContextWithoutWebAndAccounts() {
    assertThat(context.getBeanNamesForType(DispatcherServlet.class)).isEmpty();
    assertThat(context.containsBean("accountAdministration")).isFalse();
    assertThat(context.containsBean("schemaMigration")).isTrue();
  }
}
