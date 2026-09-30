package com.yanapaderina.rbot.accounts.internal.web;

import com.yanapaderina.rbot.accounts.internal.app.AccountAdministration;
import io.github.apocarteres.platform.auth.CurrentAccount;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// MVP-01, RBOT-DATA-001, REQ-AUTH-009
@RestController
@RequestMapping("/api/admin/accounts")
class AdminAccountsController {

  private final AccountAdministration administration;

  AdminAccountsController(AccountAdministration administration) {
    this.administration = administration;
  }

  @GetMapping
  AccountPage list(@RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "0") int page) {
    int current = Math.max(page, 0);
    return new AccountPage(administration.page(query, current).stream().map(AccountView::of).toList(),
      administration.count(query), current, AccountAdministration.PAGE_SIZE);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  AccountView create(@Valid @RequestBody NewAccount request) {
    return AccountView.of(administration.create(request.email(), request.password(), request.roles()));
  }

  @PutMapping("/{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void setPassword(@PathVariable UUID id, @Valid @RequestBody NewPassword request) {
    administration.setPassword(id, request.password());
  }

  @PostMapping("/{id}/block")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void block(@PathVariable UUID id) {
    administration.block(id, CurrentAccount.id());
  }

  @PostMapping("/{id}/unblock")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void unblock(@PathVariable UUID id) {
    administration.unblock(id);
  }
}
