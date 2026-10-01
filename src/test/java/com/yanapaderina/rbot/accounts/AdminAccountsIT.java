package com.yanapaderina.rbot.accounts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanapaderina.rbot.IntegrationStores;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// MVP-01, RBOT-FEAT-005, REQ-AUTH-009, REQ-AUTH-016
@SpringBootTest(properties = {
  "platform.auth.admin.email=admin@example.test",
  "platform.auth.admin.password=admin-password-1"
})
@AutoConfigureMockMvc
class AdminAccountsIT extends IntegrationStores {

  @Autowired
  private MockMvc mvc;

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
  void clientRoleIsGrantedAloneOnly() throws Exception {
    Cookie[] admin = login("admin@example.test", "admin-password-1");
    mvc.perform(post("/api/admin/accounts").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"mixed@example.test\",\"password\":\"mixed-password-1\",\"roles\":[\"CLIENT\",\"ADMIN\"]}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("roles-rejected"));
    mvc.perform(post("/api/admin/accounts").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"client@example.test\",\"password\":\"client-password-1\",\"roles\":[\"CLIENT\"]}"))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.roles[0]").value("CLIENT"));
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
  void qaSeedsAreAbsentOutsideQaProfile() throws Exception {
    mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"psychologist@yanapaderina.test\",\"password\":\"qa-psychologist-password\"}"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void failuresOfEveryStatusAreProblemDetails() throws Exception {
    Cookie[] admin = login("admin@example.test", "admin-password-1");
    mvc.perform(get("/api/admin/accounts"))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
      .andExpect(jsonPath("$.code").isNotEmpty());
    mvc.perform(post("/api/admin/accounts").cookie(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
      .andExpect(jsonPath("$.code").isNotEmpty());
    mvc.perform(get("/api/admin/nothing-here").cookie(admin))
      .andExpect(status().isNotFound())
      .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
      .andExpect(jsonPath("$.code").isNotEmpty());
    mvc.perform(post("/api/admin/accounts/00000000-0000-0000-0000-000000000009/block").cookie(admin).with(csrf()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code").value("account-missing"));
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
