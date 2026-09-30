package com.yanapaderina.rbot.accounts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yanapaderina.rbot.IntegrationStores;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// MVP-01, RUN-QA
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("qa")
class QaAccountsIT extends IntegrationStores {

  @Autowired
  private MockMvc mvc;

  @Test
  void adminSeedManagesAccounts() throws Exception {
    mvc.perform(get("/api/admin/accounts").cookie(login("admin@yanapaderina.test", "qa-admin-password")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.total").value(2));
  }

  @Test
  void psychologistSeedEntersCabinetOnly() throws Exception {
    Cookie[] psychologist = login("psychologist@yanapaderina.test", "qa-psychologist-password");
    mvc.perform(get("/api/auth/session").cookie(psychologist))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.account.roles[0]").value("PSYCHOLOGIST"));
    mvc.perform(get("/api/admin/accounts").cookie(psychologist)).andExpect(status().isForbidden());
  }

  private Cookie[] login(String email, String password) throws Exception {
    return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
      .andExpect(status().isOk())
      .andReturn().getResponse().getCookies();
  }
}
