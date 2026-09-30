package com.yanapaderina.rbot.accounts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.flywaydb.core.Flyway;

// MVP-01, REQ-AUTH-009, REQ-AUTH-016
@SpringBootTest(properties = {
  "platform.auth.admin.email=admin@example.test",
  "platform.auth.admin.password=admin-password-1",
  "platform.auth.session.cookie-secure=false"
})
@AutoConfigureMockMvc
@Testcontainers
class AdminAccountsIT {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Autowired
  private MockMvc mvc;

  @DynamicPropertySource
  static void stores(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @BeforeAll
  static void migrate() {
    Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
  }

  @Test
  void adminCreatesPsychologistWhoCannotManageAccounts() throws Exception {
    Cookie[] admin = login("admin@example.test", "admin-password-1");

    mvc.perform(post("/api/admin/accounts").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"yana@example.test\",\"password\":\"yana-password-1\",\"roles\":[\"PSYCHOLOGIST\"]}"))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.roles[0]").value("PSYCHOLOGIST"));

    mvc.perform(get("/api/admin/accounts").cookie(admin).param("query", "yana"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.total").value(1));

    Cookie[] psychologist = login("yana@example.test", "yana-password-1");
    mvc.perform(get("/api/admin/accounts").cookie(psychologist)).andExpect(status().isForbidden());
  }

  @Test
  void duplicateEmailIsRefused() throws Exception {
    Cookie[] admin = login("admin@example.test", "admin-password-1");
    mvc.perform(post("/api/admin/accounts").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"admin@example.test\",\"password\":\"other-password-1\",\"roles\":[\"ADMIN\"]}"))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("email-taken"));
  }

  @Test
  void registrationIsClosed() throws Exception {
    mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"guest@example.test\",\"password\":\"guest-password-1\"}"))
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.code").value("entry-closed"));
  }

  @Test
  void guestGetsNoAccounts() throws Exception {
    mvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
  }

  private Cookie[] login(String email, String password) throws Exception {
    return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
      .andExpect(status().isOk())
      .andReturn().getResponse().getCookies();
  }
}
