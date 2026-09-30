package com.yanapaderina.rbot.wiring;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.DispatcherServlet;

// RBOT-QUAL-003, REQ-QUALITY-012
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ApplicationWiringTest {

  @MockitoBean
  private DataSource dataSource;

  @MockitoBean(name = "springSessionRedisMessageListenerContainer")
  private RedisMessageListenerContainer sessionEvents;

  @Autowired
  private ApplicationContext context;

  @TestConfiguration
  static class WithoutRedis {

    @Bean
    ConfigureRedisAction configureRedisAction() {
      return ConfigureRedisAction.NO_OP;
    }
  }

  @Test
  void webRuntimeRaisesItsContext() {
    assertThat(context.getBeanNamesForType(DispatcherServlet.class)).isNotEmpty();
    assertThat(context.containsBean("accountAdministration")).isTrue();
  }
}
